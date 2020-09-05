package akka03

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.NotUsed

object MessageBox {

  sealed trait MessageBoxCommand
  final case class SendMessageToMessageBox(posterName: String, message: String)      extends MessageBoxCommand
  final case class UserStartsStreaming(userRef: ActorRef[User.DeliverMessageToUser]) extends MessageBoxCommand
  final case class UserStopsStreaming(userRef: ActorRef[User.DeliverMessageToUser])  extends MessageBoxCommand

  def apply(): Behavior[MessageBoxCommand] = messageBox(Set.empty[ActorRef[User.DeliverMessageToUser]])

  private def messageBox(streamingUserRef: Set[ActorRef[User.DeliverMessageToUser]]): Behavior[MessageBoxCommand] = {
    Behaviors.receive { (context, message) =>
      message match {
        case SendMessageToMessageBox(posterName, message) => {
          streamingUserRef.foreach(_ ! User.DeliverMessageToUser(posterName, message))
          Behaviors.same
        }
        case UserStartsStreaming(userRef) => messageBox(streamingUserRef + userRef)
        case UserStopsStreaming(userRef)  => messageBox(streamingUserRef - userRef)
      }
    }
  }

}

object User {

  sealed trait UserCommand
  final case class DeliverMessageToUser(posterName: String, message: String) extends UserCommand
  final case class UserSendsMessage(message: String)                         extends UserCommand
  final case object StartStreaming                                           extends UserCommand
  final case object StopStreaming                                            extends UserCommand

  def apply(userName: String, messageBox: ActorRef[MessageBox.MessageBoxCommand]): Behavior[UserCommand] =
    user(userName, messageBox)

  private def user(userName: String, messageBox: ActorRef[MessageBox.MessageBoxCommand]): Behavior[UserCommand] = {
    Behaviors.receive { (context, message) =>
      {
        message match {
          case DeliverMessageToUser(posterName, message) => {
            context.log.info("[{}]: {} said: {}", userName, posterName, message)
            Behaviors.same
          }
          case UserSendsMessage(message) => {
            messageBox ! MessageBox.SendMessageToMessageBox(userName, message)
            Behaviors.same
          }
          case StartStreaming => {
            messageBox ! MessageBox.UserStartsStreaming(context.self)
            Behaviors.same
          }
          case StopStreaming => {
            messageBox ! MessageBox.UserStopsStreaming(context.self)
            Behaviors.same
          }
        }
      }
    }
  }

}

object Main extends App {

  def createBehaviour(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val messageBox = context.spawn(MessageBox(), "messagebox")
      val user1      = context.spawn(User("Alice", messageBox), "user_1")
      val user2      = context.spawn(User("Bob", messageBox), "user_2")
      user1 ! User.StartStreaming
      user1 ! User.UserSendsMessage("hello world")
      user2 ! User.StartStreaming
      user2 ! User.UserSendsMessage("hello world2")
      user1 ! User.StopStreaming
      user2 ! User.UserSendsMessage("hello world3")

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }
  }

  ActorSystem(createBehaviour(), "messageboxmain")

}
