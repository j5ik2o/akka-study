package akka07

import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.NotUsed

// https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#request-response
object RequestResponse {

  def apply(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val cookiePrinter: ActorRef[Response] = context.spawn(CookiePrinter(), "cookie_printer")
      val cookieFabric                      = context.spawn(CookieFabric(), "cookie_fabric")

      cookieFabric ! Request("give me cookies", cookiePrinter)
      cookieFabric ! Request("give me chocolate", cookiePrinter)
      cookieFabric ! Request("give me cookies2", cookiePrinter)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }
  }

  case class Request(query: String, replyTo: ActorRef[Response])
  case class Response(result: String)

  object CookieFabric {
    def apply(): Behavior[Request] = {
      Behaviors.receive {
        case (context, Request(query, replyTo)) =>
          if (query.contains("cookie")) {
            replyTo ! Response(s"Here are the cookies for [$query]!")
          } else {
            replyTo ! Response(s"I won't give you cookies. [$query]!")
          }
          Behaviors.same
      }
    }
  }

  object CookiePrinter {
    def apply(): Behavior[Response] = {
      Behaviors.receive {
        case (context, Response(result)) =>
          context.log.info(s"[CookiePrinter]: $result")
          Behaviors.same
      }
    }
  }

}
