package storage

import actors.CborSerializable
import config.StorageConfig
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis

import java.io.*
import java.util.Base64

class RedisRecovery(config: StorageConfig.Redis) extends Recovery {
  private val logger = LoggerFactory.getLogger(getClass)

  // Using Jedis for Redis operations
  private val jedis = {
    logger.info(s"[RedisRecovery] Connecting to Redis at ${config.host}:${config.port}")
    val jedis = new Jedis(config.host, config.port)
    if (config.password.nonEmpty) {
      if (config.username.nonEmpty && config.username != "default") {
        jedis.auth(config.username, config.password)
      } else {
        jedis.auth(config.password)
      }
    }
    jedis
  }

  private def serialize(event: CborSerializable): String = {
    val baos = new ByteArrayOutputStream()
    val oos  = new ObjectOutputStream(baos)
    oos.writeObject(event)
    oos.close()
    Base64.getEncoder.encodeToString(baos.toByteArray)
  }

  private def deserialize(base64: String): CborSerializable = {
    val data = Base64.getDecoder.decode(base64)
    val ois  = new ObjectInputStream(new ByteArrayInputStream(data))
    val obj  = ois.readObject().asInstanceOf[CborSerializable]
    ois.close()
    obj
  }

  override def recover(actorId: String): List[CborSerializable] = {
    import scala.jdk.CollectionConverters.*
    logger.info(s"[RedisRecovery] Recovering events for actorId: $actorId from Redis")
    val pattern = s"recovery:$actorId:*"
    val keys    = jedis.keys(pattern).asScala.toList.sorted
    if (keys.isEmpty) {
      logger.info(s"[RedisRecovery] No recovery events found for actorId: $actorId")
      List.empty[CborSerializable]
    } else {
      logger.info(s"[RedisRecovery] Found ${keys.size} recovery events for actorId: $actorId")
      val list = jedis.mget(keys*).asScala.toList
      list.filter(_ != null).map(deserialize)
    }
  }

  override def save(actorId: String, event: CborSerializable): Unit = {
    import scala.jdk.CollectionConverters.*
    val pattern = s"recovery:$actorId:*"
    val keys    = jedis.keys(pattern)
    val nextId  = keys.size() + 1
    val newKey  = s"recovery:$actorId:$nextId"
    logger.info(s"[RedisRecovery] Saving event for actorId: $actorId to Redis key: $newKey")
    jedis.set(newKey, serialize(event))
  }

  override def delete(actorId: String): Unit = {
    import scala.jdk.CollectionConverters.*
    logger.info(s"[RedisRecovery] Deleting all recovery events for actorId: $actorId from Redis")
    val pattern = s"recovery:$actorId:*"
    val keys    = jedis.keys(pattern).asScala.toArray
    if (keys.nonEmpty) {
      logger.debug(s"[RedisRecovery] Deleting keys: ${keys.mkString(", ")}")
      jedis.del(keys*)
    }
  }

  override def doesFileExists(actorId: String): Boolean = {
    import scala.jdk.CollectionConverters.*
    val pattern = s"recovery:$actorId:*"
    !jedis.keys(pattern).isEmpty
  }
}
