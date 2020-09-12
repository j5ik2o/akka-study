package akka07

import scala.concurrent.duration._
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import scala.util.{Success, Failure}
import akka.NotUsed

// https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#request-response-with-ask-between-two-actors
object RequestResponseWithAskBetweenTwoActors {

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val hal = context.spawn(Hal(), "hal")
    /* val dave = */
    context.spawn(Dave(hal), "dave")

    Behaviors.receiveSignal {
      case (_, Terminated(_)) => Behaviors.stopped
    }
  }

  object Hal {
    sealed trait Command
    final case class OpenThePodBayDoorsPlease(replyTo: ActorRef[Response]) extends Command

    case class Response(message: String)

    // trait Receive[T] extends Behavior[T]
    def apply(): Behaviors.Receive[Hal.Command] = Behaviors.receiveMessage[Command] {
      case OpenThePodBayDoorsPlease(replyTo) => {
        replyTo ! Response("I'm sorry, Dave. I'm afraid I can't do that.")
        Behaviors.same
      }
    }
  }

  object Dave {

    sealed trait Command
    // アクター自身(Dave)内部向けのプロトコル
    // AdaptedResponseパターンで行ったものとは利用方法が異なり、context.messageAdapterは用いていない
    private case class AdaptedResponse(message: String) extends Command

    def apply(hal: ActorRef[Hal.Command]): Behavior[Dave.Command] = {
      Behaviors.setup[Command] { context =>
        implicit val timeout: Timeout = 3.seconds

        val requestId = 1
        context.ask[Hal.Command, Hal.Response](hal, Hal.OpenThePodBayDoorsPlease) {
          case Success(Hal.Response(message)) => AdaptedResponse(s"$requestId: $message")
          case Failure(_)                     => AdaptedResponse(s"$requestId: Request failed")
        }

        Behaviors.receiveMessage {
          case AdaptedResponse(message) => {
            context.log.info("Got response from hal: {}", message)
            Behaviors.same
          }
        }
      }
    }

  }

}

// ask を用いるにはタイムアウトの設定が必要。
// レスポンスが返る前にタイムアウトを迎えると ask は TimeoutException を吐いて失敗する

// context: ActorContext[T] の時、
// context.ask[Req, Res] は
// 第一引数に ActorRef[Req], ActorRef[Res] => Req
// 第二引数に Try[Res] => T
// 第三引数(implicit) に Timeout, あとClassTag[Res]

// API: https://doc.akka.io/api/akka/current/akka/actor/typed/scaladsl/ActorContext.html#ask[Req,Res](target:akka.actor.typed.RecipientRef[Req],createRequest:akka.actor.typed.ActorRef[Res]=%3EReq)(mapResponse:scala.util.Try[Res]=%3ET)(implicitresponseTimeout:akka.util.Timeout,implicitclassTag:scala.reflect.ClassTag[Res]):Unit
