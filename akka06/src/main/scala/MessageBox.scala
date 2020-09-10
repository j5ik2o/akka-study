package akka06

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object MessageBox {

  sealed trait Command
  object Command {
    final case class SendMessage(clientName: String, body: String)                    extends Command
    final case class StartStreaming(clientRef: ActorRef[ActiveClient.OutputProtocol]) extends Command
    final case class StopStreaming(clientRef: ActorRef[ActiveClient.OutputProtocol])  extends Command
  }

  def apply(): Behavior[Command] = messageBox(Set.empty[ActorRef[ActiveClient.OutputProtocol]])

  private def messageBox(
      activeClientRefs: Set[ActorRef[ActiveClient.OutputProtocol]]
  ): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case Command.SendMessage(clientName, body) => {
        val msg = ActiveClient.OutputProtocol.SendMessage(clientName, body)
        activeClientRefs.foreach(_ ! msg)
        Behaviors.same
      }
      case Command.StartStreaming(clientRef) => messageBox(activeClientRefs + clientRef)
      case Command.StopStreaming(clientRef)  => messageBox(activeClientRefs - clientRef)
    }
  }

}
