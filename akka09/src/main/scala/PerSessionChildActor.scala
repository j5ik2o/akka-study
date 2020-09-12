/*
package akka09

import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.NotUsed

// https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#per-session-child-actor
object PerSessionChildActor {

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>

    Behaviors.receiveSignal {
      case (_, Terminated(_)) => Behaviors.stopped
    }
  }

  case class Keys()
  case class Wallet()

  object Home {
    sealed trait Command
    case class LeaveHome(who: String, replyTo: ActorRef[ReadyToLeaveHome]) extends Command
    case class ReadyToLeaveHome(who: String, keys: Keys, wallet: Wallet)

    def apply(): Behavior[Command] = Behaviors.setup { context =>
      val keyCabinet: ActorRef[KeyCabinet.GetKeys] = context.spawn(KeyCabinet(), "key-cabinet")
      val drawer: ActorRef[Drawer.GetWallet] = context.spawn(Drawer(), "drawer")

      Behaviors.reveiveMesage[Command] {
        case LeaveHome(who, replyTo) => {
          context.spwan(prepareToLeaveHome(who, replyTo, keyCabinet, drawer), s"leaving-$who")
          Behaviors.same
        }
      }
    }

    def prepareToLeaveHome(
      whoIsLeaving: String,
      replyTo: ActorRef[RealyToLeaveHome],
      keyCabinet: ActorRef[KeyCabinet.GetKeys],
      drawer: ActorRef[Drawer.GetWallet]
    ): Behavior[NotUsed] = Behavior.setup[AnyRef] { context =>
      var wallet: Option[Wallet] = None
      var keys: Option[Keys] = None

      keyCabinet ! KeyCabinet.GetKeys(whoIsLeaving, context.self.narrow[Keys])
      keyCabinet ! Drawer.GetWallet(whoIsLeaving, context.self.narrow[Wallet])

      def nextBehavior(): Behavior[AnyRef] = {
        (keys, wallet) match {
          case (Some(w), Some(k)) => {
            replyTo ! ReadyToLeaveHome(whoIsLeaving, w, k)
            Behaviors.stopped
          }
          case _ => Behaviors.same
        }
      }

      Behaviors.receiveMessage {
        case w: Wallet => {
          wallet = Some(w)
          nextBehavior()
        }
        case k: Keys => {
          keys = Some(k)
          nextBehavior()
        }
        case _ => Behaviors.unhandled
      }
    }
  }

}
 */
