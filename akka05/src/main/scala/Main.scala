package akka05

import akka.stream.scaladsl.{Source, Flow, Sink, RunnableGraph, Keep}
import akka.actor.ActorSystem
import akka.{NotUsed, Done}
import scala.concurrent._
import scala.concurrent.duration._

final case class InputMessage(message: String)
final case class OutputMessage(message: String)

object Main extends App {

  implicit val system = ActorSystem("akka05")
  implicit val ec     = system.dispatcher

  val source: Source[InputMessage, NotUsed]            = Source(1 to 10).map(i => InputMessage(i.toString)).throttle(1, 100.millis)
  val flow: Flow[InputMessage, OutputMessage, NotUsed] = Flow.fromFunction(in => OutputMessage("out: " + in.message))
  val sink: Sink[OutputMessage, Future[Done]]          = Sink.foreach[OutputMessage](out => println(out.message))

  val runnable: RunnableGraph[Future[Done]] = source.via(flow).toMat(sink)(Keep.right)
  val done: Future[Done]                    = runnable.run()
  done.onComplete(_ => system.terminate())

}
