package akka06

import scala.concurrent.Future
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, Uri, HttpRequest}
import akka.http.scaladsl.model.AttributeKeys.webSocketUpgrade
import akka.http.scaladsl.model.ws
import akka.NotUsed

object WebSocket {

  def apply(webSocketApplication: Materializer => Flow[ws.Message, ws.Message, NotUsed]): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      implicit val system = context.system.classicSystem

      val requestHandler: HttpRequest => HttpResponse = {
        case req @ HttpRequest(GET, Uri.Path("/greeter"), _, _, _) =>
          req.attribute(webSocketUpgrade) match {
            case Some(upgrade) => upgrade.handleMessages(webSocketApplication(implicitly[Materializer]))
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

      Behaviors.ignore
    }
  }

}
