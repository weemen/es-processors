package actors

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.Eventually
import processors.{BaseProcessor, TestEventA, TestEventB}
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration.*

class MockProcessingProcessor(listOfEvents: List[Any]) extends BaseProcessor(listOfEvents) {
  var processCalledCount = 0
  override def process(): Option[Any] = {
    processCalledCount += 1
    for {
      a <- getEventByType[TestEventA]
      b <- getEventByType[TestEventB]
    } yield s"Processed-${a.value}-${b.value}"
  }
}

class ProcessingActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with Eventually {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds, interval = 100.millis)

  "ProcessingActor" should {

    "register events and call process on every message" in {
      val processor = new MockProcessingProcessor(List(TestEventA, TestEventB))
      val actor = spawn(ProcessingActor("test-actor-1", processor))
      
      val eventA = TestEventA("A")
      actor ! eventA
      
      eventually {
        processor.getEventByType[TestEventA] shouldBe Some(eventA)
        processor.processCalledCount shouldBe 1
      }
      
      val eventB = TestEventB(2)
      actor ! eventB
      
      eventually {
        processor.getEventByType[TestEventB] shouldBe Some(eventB)
        processor.processCalledCount shouldBe 2
      }
    }

    "not register events not in the list" in {
      val processor = new MockProcessingProcessor(List(TestEventA))
      val actor = spawn(ProcessingActor("test-actor-2", processor))
      
      actor ! TestEventB(2)
      
      eventually {
        processor.getEventByType[TestEventB] shouldBe None
        processor.processCalledCount shouldBe 1
      }
    }
  }
}
