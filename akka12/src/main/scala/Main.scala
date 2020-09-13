package akka12

import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

object Main extends App {

  implicit val wrapperActorSystem = ActorSystem(Wrapper(), "main")

  implicit val timeout: Timeout = 3.seconds
  implicit val ec               = wrapperActorSystem.executionContext

  println(s"\npress ENTER to terminate.\n")

  var active = true
  var count  = 0

  Future {
    while (active) {
      count += 1
      val mes = s"ya $count"
      println(mes)
      val result: Future[String] = wrapperActorSystem.askWithStatus(Wrapper.RequestMessage(mes, _))
      result.onComplete {
        case Success(value) => println(s"echo: $value")
        case Failure(e)     => println(s"error: ${e.getMessage}")
      }
      Thread.sleep(200)
    }
  }

  io.StdIn.readLine()
  active = false
  wrapperActorSystem.terminate()

}
