package akka09

import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import akka.pattern.StatusReply
import akka.NotUsed

// https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#generic-response-wrapper
object GenericResponseWrapper {

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val hal: ActorRef[Hal.Command] = context.spawn(Hal(), "hal")
    /* val dave: ActorRef[Dave.Command] = */
    context.spawn(Dave(hal), "dave")

    Behaviors.receiveSignal {
      case (_, Terminated(_)) => Behaviors.stopped
    }
  }

  object Hal {
    sealed trait Command
    case class OpenThePodBayDoorsPlease(replyTo: ActorRef[StatusReply[String]]) extends Command

    def apply(): Behaviors.Receive[Hal.Command] = Behaviors.receiveMessage[Command] {
      case OpenThePodBayDoorsPlease(replyTo) => {
        replyTo ! StatusReply.Error("I'm sorry, Dave. I'm afraid I can't do that.")
        Behaviors.same
      }
    }
  }

  object Dave {
    sealed trait Command
    private case class AdaptedResponse(message: String) extends Command

    def apply(hal: ActorRef[Hal.Command]): Behavior[Dave.Command] = {
      Behaviors.setup[Command] { context =>
        implicit val timeout: Timeout = 3.seconds

        context.askWithStatus(hal, Hal.OpenThePodBayDoorsPlease) {
          case Success(message)                        => AdaptedResponse(message)
          case Failure(StatusReply.ErrorMessage(text)) => AdaptedResponse(s"Request denied: $text")
          case Failure(_)                              => AdaptedResponse("Request failed")
        }

        Behaviors.receiveMessage {
          case AdaptedResponse(message) => {
            context.log.info(s"Got response from hal: $message")
            Behaviors.same
          }
        }
      }
    }
  }

}
