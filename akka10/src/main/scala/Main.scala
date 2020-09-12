package akka10

import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.NotUsed

object Main extends App {

  def apply(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val printer = context.spawn(Printer(), "printer")

      val after1second = context.spawn(After(1000), "after_1_second")
      val after2second = context.spawn(After(2000), "after_2_second")
      val after4second = context.spawn(After(4000), "after_4_second")

      after1second ! After.RequestMessage("hello1-1", printer)
      after1second ! After.RequestMessage("hello1-2", printer)
      after2second ! After.RequestMessage("hello2-1", printer)
      after2second ! After.RequestMessage("hello2-2", printer)
      after4second ! After.RequestMessage("hello3-1", printer)
      after4second ! After.RequestMessage("hello3-2", printer)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }
  }

  val system = ActorSystem(Main(), "main")

  println("\npress ENTER to terminate.\n")

  io.StdIn.readLine()
  system.terminate()

}
