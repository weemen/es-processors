package processors

import actors.CborSerializable
import org.apache.pekko.Done
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.apache.pekko.actor.typed.ActorRef

case class TestEventA(value: String)  extends CborSerializable
case class TestEventB(value: Int)     extends CborSerializable
case class TestEventC(value: Boolean) extends CborSerializable

class MockProcessor(listOfEvents: List[Any]) extends BaseProcessor(listOfEvents) {
  override def process(): Option[Done] = {
    for {
      a <- getEventByType[TestEventA]
      b <- getEventByType[TestEventB]
    } yield Done
  }
}

class BaseProcessorSpec extends AnyWordSpec with Matchers {

  "BaseProcessor" should {

    "register events that are in the list of events" in {
      val processor = new MockProcessor(List(TestEventA, TestEventB))
      val eventA    = TestEventA("test")

      processor.registerEvent(eventA)
      processor.getEventByType[TestEventA] shouldBe Some(eventA)
    }

    "not register events that are not in the list of events" in {
      val processor = new MockProcessor(List(TestEventA))
      val eventB    = TestEventB(123)

      processor.registerEvent(eventB)
      processor.getEventByType[TestEventB] shouldBe None
    }

    "not register duplicate events of the same type" in {
      val processor = new MockProcessor(List(TestEventA))
      val eventA1   = TestEventA("first")
      val eventA2   = TestEventA("second")

      processor.registerEvent(eventA1)
      processor.registerEvent(eventA2)

      processor.getEventByType[TestEventA] shouldBe Some(eventA1)
    }

    "correctly retrieve events by type" in {
      val processor = new MockProcessor(List(TestEventA, TestEventB))
      val eventA    = TestEventA("test")
      val eventB    = TestEventB(42)

      processor.registerEvent(eventA)
      processor.registerEvent(eventB)

      processor.getEventByType[TestEventA] shouldBe Some(eventA)
      processor.getEventByType[TestEventB] shouldBe Some(eventB)
    }

    "yield result when process is called and required events are present" in {
      val processor = new MockProcessor(List(TestEventA, TestEventB))
      processor.registerEvent(TestEventA("hello"))
      processor.registerEvent(TestEventB(100))

      processor.process() shouldBe Some(Done)
    }

    "return None when process is called but events are missing" in {
      val processor = new MockProcessor(List(TestEventA, TestEventB))
      processor.registerEvent(TestEventA("hello"))

      processor.process() shouldBe None
    }

    "handle registration using Class objects in listOfEvents" in {
      val processor = new MockProcessor(List(classOf[TestEventA]))
      val eventA    = TestEventA("classTest")

      processor.registerEvent(eventA)
      processor.getEventByType[TestEventA] shouldBe Some(eventA)
    }
  }
}
