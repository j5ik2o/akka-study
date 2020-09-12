package akka08

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

// Request-Response with ask from outside an Actor
// https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#request-response-with-ask-from-outside-an-actor
object CookieFabric {
  sealed trait Command
  case class GimeMeCookies(count: Int, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply
  case class Cookies(count: Int)            extends Reply
  case class InvalidRequest(reason: String) extends Reply

  def apply(): Behaviors.Receive[CookieFabric.GimeMeCookies] = Behaviors.receiveMessage { message =>
    if (message.count >= 5) message.replyTo ! InvalidRequest("Too many cookies.")
    else message.replyTo ! Cookies(message.count)

    Behaviors.same
  }
}
