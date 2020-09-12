package akka09

import scala.util.{Success, Failure}
import scala.concurrent.Future
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.{NotUsed, Done}

// https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#send-future-result-to-self
object SendFutureResultToSelf {

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val customerDataAccess: CustomerDataAccess = ???
    val customerRepository: ActorRef[CustomerRepository.Command] =
      context.spawn(CustomerRepository(customerDataAccess), "customer_repository")

    val customer = Customer("id1", 0, "Alice", "alice_address")

    customerRepository ! CustomerRepository.Update(customer, context.system.ignoreRef)

    Behaviors.receiveSignal {
      case (_, Terminated(_)) => Behaviors.stopped
    }
  }

  trait CustomerDataAccess {
    def update(value: Customer): Future[Done]
  }

  final case class Customer(id: String, version: Long, name: String, address: String)

  object CustomerRepository {

    sealed trait Command
    final case class Update(value: Customer, replyTo: ActorRef[UpdateResult]) extends Command

    sealed trait UpdateResult
    final case class UpdateSuccess(id: String)                 extends UpdateResult
    final case class UpdateFailure(id: String, reason: String) extends UpdateResult

    private final case class WrappedUpdateResult(result: UpdateResult, replyTo: ActorRef[UpdateResult]) extends Command

    private val MaxOperationsInProgress = 10

    def apply(dataAccess: CustomerDataAccess): Behavior[Command] = {
      next(dataAccess, operationsInProgress = 0)
    }

    private def next(dataAccess: CustomerDataAccess, operationsInProgress: Int): Behavior[Command] = {
      Behaviors.receive { (context, command) =>
        command match {
          case Update(value, replyTo) => {
            if (operationsInProgress == MaxOperationsInProgress) {
              replyTo ! UpdateFailure(value.id, s"Max $MaxOperationsInProgress concurrent operations supported")
              Behaviors.same
            } else {
              val futureResult: Future[Done] = dataAccess.update(value)
              context.pipeToSelf(futureResult) {
                case Success(_) => WrappedUpdateResult(UpdateSuccess(value.id), replyTo)
                case Failure(e) => WrappedUpdateResult(UpdateFailure(value.id, e.getMessage), replyTo)
              }

              next(dataAccess, operationsInProgress + 1)
            }
          }

          case WrappedUpdateResult(result, replyTo) => {
            replyTo ! result
            next(dataAccess, operationsInProgress - 1)
          }
        }
      }
    }

  }

}
