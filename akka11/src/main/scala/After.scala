package akka11

import scala.util.{Try, Success, Failure}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern

object After {

  sealed trait Request[T]
  final case class RequestMessage[T](
      message: T,
      delay: FiniteDuration,
      replyTo: ActorRef[Try[T]]
  ) extends Request[T]
  private final case class InternalAdapter[T](status: Try[T], replyTo: ActorRef[Try[T]]) extends Request[T]

  def apply[T](): Behavior[Request[T]] = after()

  def after[T](): Behavior[Request[T]] = Behaviors.receive { (context, message) =>
    implicit val classicSystem = context.system.classicSystem

    message match {
      case r: RequestMessage[T] => {
        val delayed: Future[RequestMessage[T]] = pattern.after(r.delay)(Future.successful(r))
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

object Afters {

  sealed trait Request[T]
  case class RequestMessage[T](
      message: T,
      delays: Seq[FiniteDuration],
      replyTo: ActorRef[Try[T]]
  ) extends Request[T]

  def apply[T](): Behavior[Request[T]] = afters()

  def afters[T](): Behavior[Request[T]] = Behaviors.receive { (context, message) =>
    message match {
      case RequestMessage(message, delays, replyTo) => {
        val after = context.spawnAnonymous(After[T]())
        delays.foreach { d => after ! After.RequestMessage[T](message, d, replyTo) }
      }
    }
    Behaviors.same
  }

}

object Printer {

  def apply[T](): Behavior[Try[T]] = Behaviors.receive { (context, message) =>
    message match {
      case Success(message) => context.log.info(s"success: $message")
      case Failure(e)       => context.log.info(s"failure: ${e.getMessage}")
    }
    Behaviors.same
  }

}
