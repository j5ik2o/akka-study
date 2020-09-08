package akka06

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Sink
import akka.Done
import scala.concurrent.Future

object Printer {

  def apply(): Behavior[String] = messagePrinter()

  private def messagePrinter(): Behavior[String] =
    Behaviors.receive { (context, message) =>
      context.log.info("message: {}", message)
      Behaviors.same
    }

  def createPrinterSink(printerRef: ActorRef[String]): Sink[String, Future[Done]] = {
    Sink.foreach[String](printerRef ! _)
  }

}
