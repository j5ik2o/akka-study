package akka06

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.NotUsed
import scala.util.Random

object ActiveClient {

  sealed trait Protocol
  sealed trait InputProtocol extends Protocol
  object InputProtocol {
    final case class SendMessage(body: String) extends InputProtocol
  }
  sealed trait OutputProtocol extends Protocol
  object OutputProtocol {
    final case class SendMessage(name: String, body: String) extends OutputProtocol
  }

  def apply(
      clientName: String,
      replyTo: ActorRef[OutputProtocol],
      messageBox: ActorRef[MessageBox.Command]
  ): Behavior[Protocol] = {
    messageBox ! MessageBox.Command.StartStreaming(replyTo)
    activeClient(clientName, replyTo, messageBox)
  }

  // helper
  def generate(
      messageBox: ActorRef[MessageBox.Command],
      context: ActorContext[NotUsed]
  ): ActorRef[OutputProtocol] => ActorRef[InputProtocol] = { replyTo: ActorRef[OutputProtocol] =>
    val cn = randomClientName()
    val b  = apply(cn, replyTo, messageBox)
    context.spawn(b, cn)
  }

  private def activeClient(
      clientName: String,
      replyTo: ActorRef[OutputProtocol],
      messageBox: ActorRef[MessageBox.Command]
  ): Behavior[Protocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case ip: InputProtocol =>
          ip match {
            case InputProtocol.SendMessage(body) => {
              messageBox ! MessageBox.Command.SendMessage(clientName, body)
              Behaviors.same
            }
          }
        case op: OutputProtocol => {
          replyTo ! op
          Behaviors.same
        }
      }
    }

  private def randomClientName(): String = s"client-${Random.nextInt(100)}"

}
