package akka11

import scala.util.{Try, Success, Failure}
import scala.concurrent.duration._

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply

object DelayEcho {

  sealed trait Request
  final case class RequestMessage(message: String, replyTo: ActorRef[StatusReply[String]]) extends Request
  final case object Stop                                                                   extends Request
  private final case class WrappedAfterResponse(response: Try[String], replyTo: ActorRef[StatusReply[String]])
      extends Request

  val DelayMillis = 1000.millis

  def apply(): Behavior[Request] = Behaviors.setup { context =>
    val after = context.spawn(After[String](), "after")

    Behaviors.receiveMessage {
      case RequestMessage(message, replyTo) => {
        after ! After
          .RequestMessage[String](message, DelayMillis, context.messageAdapter(WrappedAfterResponse(_, replyTo)))
        Behaviors.same
      }
      case WrappedAfterResponse(response, replyTo) => {
        response match {
          case Success(value)     => replyTo ! StatusReply.Success(value)
          case Failure(exception) => replyTo ! StatusReply.Error(exception)
        }
        Behaviors.same
      }
      case Stop => {
        context.log.info("bye.")
        Behaviors.stopped
      }
    }
  }

}
