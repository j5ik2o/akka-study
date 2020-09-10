package akka07

import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.NotUsed

// https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#fire-and-forget
object FireAndForget {

  def apply(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val printer: ActorRef[Printer.PrintMe] = context.spawn(Printer(), "printer")

      printer ! Printer.PrintMe("message 1")
      printer ! Printer.PrintMe("message 2")

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }
  }

}

object Printer {

  case class PrintMe(message: String)

  def apply(): Behavior[PrintMe] = {
    Behaviors.receive {
      case (context, PrintMe(message)) =>
        context.log.info("[Printer]: {}", message)
        Behaviors.same
    }
  }

}
