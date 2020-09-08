package akka06

import scala.concurrent.Future
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.http.scaladsl.model.ws.{TextMessage, BinaryMessage, Message}
import akka.{Done, NotUsed}
import akka06.ActiveClient.OutputProtocol.SendMessage

object WebSocketApplication {

  val greeterWebSocketService = { implicit mat: Materializer =>
    Flow[Message]
      .mapConcat {
        case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream) :: Nil
        case bm: BinaryMessage =>
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
  }

  def inputCodec(mat: Materializer): Flow[Message, ActiveClient.InputProtocol, NotUsed] = {
    implicit val m = mat
    Flow[Message]
      .flatMapConcat {
        case tm: TextMessage => tm.textStream.map(ActiveClient.InputProtocol.SendMessage)
        case bm: BinaryMessage =>
          bm.dataStream.runWith(Sink.ignore)
          Source.empty
      }
  }
  val outputCodec: Flow[ActiveClient.OutputProtocol, Message, NotUsed] = {
    Flow[ActiveClient.OutputProtocol]
      .flatMapConcat {
        case SendMessage(name, body) => Source.single(TextMessage(s"[$name]: $body"))
      }
  }

  def myApplication(context: ActorContext[NotUsed]): Materializer => Flow[Message, Message, NotUsed] = {
    val messageBox = context.spawn(MessageBox(), "messagebox")

    (mat: Materializer) => {
      var client: ActorRef[ActiveClient.InputProtocol] = null
      val (flow, replyTo)                              = webSocketAdapter(client, inputCodec(mat), outputCodec)
      client = ActiveClient.generate(messageBox, context)(replyTo)
      flow
    }
  }

  def webSocketAdapter[In, Out](
      client: ActorRef[In],
      inputCodec: Flow[Message, In, NotUsed],
      outputCodec: Flow[Out, Message, NotUsed]
  ): (Flow[Message, Message, NotUsed], ActorRef[Out]) = {

    var replyTo: ActorRef[Out] = null

    val sink: Sink[In, Future[Done]] = Sink.foreach[In](in => client ! in)
    val source: Source[Out, ActorRef[Out]] = ActorSource
      .actorRef[Out](
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        bufferSize = 8,
        overflowStrategy = OverflowStrategy.fail
      )
      .mapMaterializedValue { actor =>
        replyTo = actor
        actor
      }

    val fl: Flow[In, Out, NotUsed]            = Flow.fromSinkAndSource(sink, source)
    val flow: Flow[Message, Message, NotUsed] = inputCodec.via(fl).via(outputCodec)

    (flow, replyTo)
  }
}
