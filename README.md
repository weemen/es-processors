# Simple Processors for Eventsourcing with Scala
This repository contains helpers to create simple processors designed for event sourcing applications using Scala. 
The processors are designed to be lightweight, flexible, and easy to integrate into existing event sourcing systems.

## Sample usage:
```scala
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.util.Timeout
import processors.BaseProcessor
import actors.{ProcessorManagerActor, RegisterProcessor, ProcessEvent, CborSerializable}

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final case class DomainEventA(myPropertyA: String, myPropertyB: Int) extends CborSerializable
final case class DomainEventB(myPropertyX: String, myPropertyY: Int) extends CborSerializable
final case class DomainEventC(myPropertyC: String, myPropertyD: Int) extends CborSerializable

class SomeProcessorType(listOfEvents: List[Any]) extends BaseProcessor(listOfEvents):

def process(): Option[DomainEventC] = {
  for
    eventA <- getEventByType[DomainEventA]
    eventB <- getEventByType[DomainEventB]
  yield DomainEventC(myPropertyC = "C", myPropertyD = eventA.myPropertyB + eventB.myPropertyY)
}

@main
def main(): Unit = {
  val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "TaggedActorsTyped")

  val field_to_match_1 = UUID.randomUUID().toString
  val field_to_match_2 = UUID.randomUUID().toString

  val eventA = DomainEventA(field_to_match_1, 1)
  val eventB = DomainEventB(field_to_match_1, 2)
  val eventC = DomainEventC(field_to_match_2, 2)

  val processor = new SomeProcessorType(listOfEvents = List(classOf[DomainEventA], classOf[DomainEventB]))

  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler = system.scheduler



  val managerActorFuture = system.ask[ActorRef[Any]](replyTo =>
    SpawnProtocol.Spawn(ProcessorManagerActor("processorManager"), "processorManager", Props.empty, replyTo)
  )
  val managerActor = Await.result(managerActorFuture, timeout.duration)
  println("Manager actor spawned")
  managerActor ! RegisterProcessor(processor)
  managerActor ! ProcessEvent(eventA, field_to_match_1)
  managerActor ! ProcessEvent(eventB, field_to_match_1) // Use same ID as event
  system.terminate()
}
```