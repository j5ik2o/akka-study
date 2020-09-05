package akka04

import akka.stream.scaladsl._
import akka.actor.ActorSystem
import scala.concurrent._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.AttributeKeys.webSocketUpgrade
import akka.http.scaladsl.model.ws.{TextMessage, BinaryMessage, Message}
import akka.http.scaladsl.model.{HttpResponse, Uri, HttpRequest}
import akka.http.scaladsl.model.HttpMethods._

object Main extends App {

  implicit val system           = ActorSystem()
  implicit val executionContext = system.dispatcher

  val greeterWebSocketService =
    Flow[Message]
      .mapConcat {
        case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream) :: Nil
        case bm: BinaryMessage =>
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }

  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(GET, Uri.Path("/greeter"), _, _, _) =>
      req.attribute(webSocketUpgrade) match {
        case Some(upgrade) => upgrade.handleMessages(greeterWebSocketService)
        case None          => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().newServerAt("localhost", 8080).connectionSource()
  val bindingFuture: Future[Http.ServerBinding] =
    serverSource
      .to(Sink.foreach { connection => // foreach materializes the source
        println("Accepted new connection from " + connection.remoteAddress)
        connection handleWithSyncHandler requestHandler
      })
      .run()

}
