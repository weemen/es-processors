package storage

import actors.CborSerializable
import config.StorageConfig
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import java.io.File
import java.nio.file.Files

case class RecoveryTestEvent(id: Int, name: String) extends CborSerializable

class LocalRecoverySpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private var tempDir: File           = _
  private var recovery: LocalRecovery = _

  override def beforeEach(): Unit = {
    tempDir = Files.createTempDirectory("local-recovery-test").toFile
    recovery = new LocalRecovery(StorageConfig.Local(tempDir.getAbsolutePath))
  }

  override def afterEach(): Unit = {
    def deleteDir(file: File): Unit = {
      if (file.isDirectory) {
        file.listFiles().foreach(deleteDir)
      }
      file.delete()
    }
    deleteDir(tempDir)
  }

  "LocalRecovery" should {

    "successfully save and recover an event" in {
      val actorId = "actor-1"
      val event   = RecoveryTestEvent(1, "test-event")

      recovery.save(actorId, event)

      recovery.doesFileExists(actorId) shouldBe true

      val recoveredEvents = recovery.recover(actorId)
      recoveredEvents should have size 1
      recoveredEvents.head shouldBe event
    }

    "successfully save and recover multiple events in order" in {
      val actorId = "actor-2"
      val event1  = RecoveryTestEvent(1, "first")
      val event2  = RecoveryTestEvent(2, "second")
      val event3  = RecoveryTestEvent(3, "third")

      recovery.save(actorId, event1)
      recovery.save(actorId, event2)
      recovery.save(actorId, event3)

      val recoveredEvents = recovery.recover(actorId)
      recoveredEvents shouldBe List(event1, event2, event3)
    }

    "successfully delete all events for an actor" in {
      val actorId = "actor-3"
      val event1  = RecoveryTestEvent(1, "first")
      val event2  = RecoveryTestEvent(2, "second")

      recovery.save(actorId, event1)
      recovery.save(actorId, event2)

      recovery.doesFileExists(actorId) shouldBe true

      recovery.delete(actorId)

      recovery.doesFileExists(actorId) shouldBe false
      recovery.recover(actorId) shouldBe empty
    }

    "return false for doesFileExists if no events are saved" in {
      recovery.doesFileExists("non-existent-actor") shouldBe false
    }

    "return empty list for recover if no events are saved" in {
      recovery.recover("non-existent-actor") shouldBe empty
    }
  }
}
