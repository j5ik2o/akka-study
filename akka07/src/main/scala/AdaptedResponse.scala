package akka07

import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.NotUsed
import java.net.URI

// https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#adapted-response
object AdaptedResponse {

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val printer: ActorRef[URI]               = context.spawn(URIPrinter(), "uri_printer")
    val backend: ActorRef[Backend.Request]   = ???
    val frontend: ActorRef[Frontend.Command] = context.spawn(Frontend(backend), "frontend")

    frontend ! Frontend.Translate(new URI("http://example.com"), printer)

    Behaviors.receiveSignal {
      case (_, Terminated(_)) => Behaviors.stopped
    }
  }

  object Backend {
    sealed trait Request
    final case class StartTranslationJob(taskId: Int, site: URI, replyTo: ActorRef[Response]) extends Request

    sealed trait Response
    final case class JobStarted(taskId: Int)                    extends Response
    final case class JobProgress(taskId: Int, progress: Double) extends Response
    final case class JobCompleted(taskId: Int, result: URI)     extends Response
  }

  object Frontend {

    sealed trait Command
    final case class Translate(site: URI, replyTo: ActorRef[URI])               extends Command
    private final case class WrappedBackendResponse(response: Backend.Response) extends Command

    def apply(backend: ActorRef[Backend.Request]): Behavior[Command] = Behaviors.setup[Command] { context =>
      val backendResponseMapper: ActorRef[Backend.Response] =
        context.messageAdapter(rsp => WrappedBackendResponse(rsp))

      def active(inProgress: Map[Int, ActorRef[URI]], count: Int): Behavior[Command] = {
        Behaviors.receiveMessage[Command] {
          case Translate(site, replyTo) => {
            val taskId = count + 1
            backend ! Backend.StartTranslationJob(taskId, site, backendResponseMapper)
            active(inProgress.updated(taskId, replyTo), taskId)
          }
          case WrappedBackendResponse(response) =>
            response match {
              case Backend.JobStarted(taskId) => {
                context.log.info(s"Started $taskId")
                Behaviors.same
              }
              case Backend.JobProgress(taskId, progress) => {
                context.log.info(s"Progress $taskId: $progress")
                Behaviors.same
              }
              case Backend.JobCompleted(taskId, result) => {
                context.log.info(s"Completed $taskId: $result")
                inProgress(taskId) ! result
                active(inProgress - taskId, count)
              }
            }
        }
      }

      active(inProgress = Map.empty, count = 0)
    }

  }

  object URIPrinter {
    def apply(): Behavior[URI] = Behaviors.receive {
      case (context, uri) =>
        context.log.info(s"[URIPrinter]: ${uri.toString}")
        Behaviors.same
    }

  }

}
