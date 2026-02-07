package actors

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.Eventually
import processors.{BaseProcessor, TestEventA, TestEventB}
import scala.concurrent.duration.*
import org.apache.pekko.actor.typed.{Behavior, SpawnProtocol}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

class MockManagerProcessor(val name: String, listOfEvents: List[Any]) extends BaseProcessor(listOfEvents) {
  var lastRegisteredEvent: Option[Any]          = None
  override def registerEvent[A](event: A): Unit = {
    lastRegisteredEvent = Some(event)
    super.registerEvent(event)
  }
  override def process(): Option[Any]           = None
  override def toString: String                 = s"MockManagerProcessor($name)"
}

class ProcessorManagerActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with Eventually {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds, interval = 100.millis)

  "ProcessorManagerActor" should {

    "register a processor" in {
      val manager   = spawn(ProcessorManagerActor("manager-1"))
      val processor = new MockManagerProcessor("p1", List(TestEventA))

      manager ! RegisterProcessor(processor)
    }

    "broadcast events to registered processors" in {
      val manager   = spawn(ProcessorManagerActor("manager-2"))
      val processor = new MockManagerProcessor("p2", List(TestEventA))

      manager ! RegisterProcessor(processor)

      val eventA = TestEventA("test-broadcast")
      manager ! ProcessEvent(eventA, "identifier-1")

      eventually {
        processor.lastRegisteredEvent shouldBe Some(eventA)
      }
    }

    "not broadcast events to processors that don't need them" in {
      val manager   = spawn(ProcessorManagerActor("manager-3"))
      val processor = new MockManagerProcessor("p3", List(TestEventA))

      manager ! RegisterProcessor(processor)

      val eventB = TestEventB(42)
      manager ! ProcessEvent(eventB, "identifier-2")

      // Give it some time to ensure it wasn't registered
      Thread.sleep(500)
      processor.lastRegisteredEvent shouldBe None
    }

    "handle multiple processors" in {
      val manager    = spawn(Behaviors.setup[Any] { ctx =>
        ctx.log.info("Starting isolated manager for multi-processor test")
        ProcessorManagerActor("manager-4")
      })
      val processor1 = new MockManagerProcessor("multi-p1", List(TestEventA))
      val processor2 = new MockManagerProcessor("multi-p2", List(TestEventB))

      manager ! RegisterProcessor(processor1)
      manager ! RegisterProcessor(processor2)

      val eventA = TestEventA("A")
      val eventB = TestEventB(2)

      manager ! ProcessEvent(eventA, "id-A")
      manager ! ProcessEvent(eventB, "id-B")

      eventually {
        processor1.lastRegisteredEvent shouldBe Some(eventA)
        processor2.lastRegisteredEvent shouldBe Some(eventB)
      }
    }
  }
}
