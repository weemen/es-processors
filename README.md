# Simple Processors for Eventsourcing with Scala
This repository contains helpers to create simple processors designed for event sourcing applications using Scala. 
The processors are designed to be lightweight, flexible, and easy to integrate into existing event sourcing systems.

## Sample usage:
```scala
package weemen

import actors.{ProcessEvent, ProcessorManagerActor, RegisterProcessor}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.util.Timeout
import processors.BaseProcessor

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final case class DomainEventA(myPropertyA: String, myPropertyB: Int)
final case class DomainEventB(myPropertyX: String, myPropertyY: Int)
final case class DomainEventC(myPropertyC: String, myPropertyD: Int)

class SomeProcessorType(listOfEvents: List[Any]) extends BaseProcessor(listOfEvents):

  def process(): Option[DomainEventC] = {
    for
      eventA <- getEventByType[DomainEventA]
      eventB <- getEventByType[DomainEventB]
    yield DomainEventC(myPropertyC = "C", myPropertyD = eventA.myPropertyB + eventB.myPropertyY)
  }

object SimpleESProcessor {
  def main(args: Array[String]): Unit = {
    val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "TaggedActorsTyped")
    println("Hello, world!")

    val some_fileid  = UUID.randomUUID().toString
    val other_fileid = UUID.randomUUID().toString

    val eventA = DomainEventA(some_fileid, 1)
    val eventB = DomainEventB(some_fileid, 2)
    val eventX = DomainEventC(other_fileid, 2)

    val someEvent: Any = eventA

    val processor = new SomeProcessorType(listOfEvents = List(DomainEventA, DomainEventB))

    implicit val timeout: Timeout = 3.seconds
    implicit val scheduler        = system.scheduler

    println(s"Target actor IDs: ${eventA.myPropertyA}")

    val managerActorFuture = system.ask[ActorRef[Any]](replyTo =>
      SpawnProtocol.Spawn(ProcessorManagerActor("processorManager"), "processorManager", Props.empty, replyTo)
    )
    val managerActor       = Await.result(managerActorFuture, timeout.duration)
    println("Manager actor spawned")
    managerActor ! RegisterProcessor(processor)
    managerActor ! ProcessEvent(eventA, eventA.myPropertyA)
    managerActor ! ProcessEvent(eventB, eventB.myPropertyX) // Use same ID as event
    system.terminate()
  }
}
```