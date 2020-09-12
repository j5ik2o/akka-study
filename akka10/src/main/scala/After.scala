package akka10

import scala.util.{Try, Success, Failure}
import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern

object After {

  sealed trait Request
  final case class RequestMessage(message: String, replyTo: ActorRef[Try[String]])              extends Request
  private final case class InternalAdapter(status: Try[String], replyTo: ActorRef[Try[String]]) extends Request

  def apply(delayMillis: Long): Behavior[Request] = after(delayMillis)

  def after(delayMillis: Long): Behavior[Request] = Behaviors.receive { (context, message) =>
    implicit val classicSystem        = context.system.classicSystem
    val delayDuration: FiniteDuration = delayMillis.millis

    message match {
      case r: RequestMessage => {
        val delayed: Future[RequestMessage] = pattern.after(delayDuration)(Future.successful(r))
        context.pipeToSelf(delayed) {
          case t => InternalAdapter(t.map(_.message), r.replyTo)
        }
      }
      case InternalAdapter(status, replyTo) => {
        replyTo ! status
      }
    }
    Behaviors.same
  }

}

object Printer {

  def apply(): Behavior[Try[String]] = Behaviors.receive { (context, message) =>
    message match {
      case Success(message) => context.log.info(s"success: $message")
      case Failure(e)       => context.log.info(s"failure: ${e.getMessage}")
    }
    Behaviors.same
  }

}
