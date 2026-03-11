package storage

import actors.CborSerializable
import config.StorageConfig
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import redis.clients.jedis.Jedis
import scala.compiletime.uninitialized

case class RedisRecoveryTestEvent(id: Int, name: String) extends CborSerializable

class RedisRecoverySpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val redisConfig: StorageConfig.Redis = StorageConfig.Redis(
    username = "es-processors-sagas",
    password = "es-processors-sagas",
    host = "localhost",
    port = 6379
  )
  private var recovery: RedisRecovery          = uninitialized
  private var jedis: Jedis                     = uninitialized

  override def beforeEach(): Unit = {
    recovery = new RedisRecovery(redisConfig)
    jedis = new Jedis(redisConfig.host, redisConfig.port)
    jedis.auth(redisConfig.username, redisConfig.password)
    // Clear all keys before each test
    jedis.flushAll()
  }

  override def afterEach(): Unit = {
    if (jedis != null) {
      jedis.close()
    }
  }

  "RedisRecovery" should {

    "successfully save and recover an event" in {
      val actorId = "actor-1"
      val event   = RedisRecoveryTestEvent(1, "test-event")

      recovery.save(actorId, event)

      recovery.doesFileExists(actorId) shouldBe true

      val recoveredEvents = recovery.recover(actorId)
      recoveredEvents should have size 1
      recoveredEvents.head shouldBe event
    }

    "successfully save and recover multiple events in order" in {
      val actorId = "actor-2"
      val event1  = RedisRecoveryTestEvent(1, "first")
      val event2  = RedisRecoveryTestEvent(2, "second")
      val event3  = RedisRecoveryTestEvent(3, "third")

      recovery.save(actorId, event1)
      recovery.save(actorId, event2)
      recovery.save(actorId, event3)

      val recoveredEvents = recovery.recover(actorId)
      recoveredEvents shouldBe List(event1, event2, event3)
    }

    "successfully delete all events for an actor" in {
      val actorId = "actor-3"
      val event1  = RedisRecoveryTestEvent(1, "first")
      val event2  = RedisRecoveryTestEvent(2, "second")

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
