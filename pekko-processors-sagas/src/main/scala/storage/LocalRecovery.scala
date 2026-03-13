package storage

import actors.CborSerializable
import config.StorageConfig
import org.slf4j.LoggerFactory

import java.io.*

class LocalRecovery(config: StorageConfig.Local) extends Recovery {
  private val logger = LoggerFactory.getLogger(getClass)

  override def recover(actorId: String): List[CborSerializable] = {
    logger.info(s"[LocalRecovery] Recovering events for actorId: $actorId from path: ${config.storagePath}")
    var events      = List.empty[CborSerializable]
    var eventNumber = 0
    var keepGoing   = true

    while (keepGoing) {
      val file = new File(s"${config.storagePath}/$actorId-$eventNumber.bin")
      if (file.exists()) {
        logger.debug(s"[LocalRecovery] Found recovery file: ${file.getPath}")
        val ois   = new ObjectInputStream(new FileInputStream(file))
        val event = ois.readObject().asInstanceOf[CborSerializable]
        events = events :+ event
        ois.close()
        eventNumber += 1
      } else {
        keepGoing = false
      }
    }
    logger.info(s"[LocalRecovery] Recovered ${events.size} events for actorId: $actorId")
    events
  }

  override def save(actorId: String, event: CborSerializable): Unit = {
    var eventNumber = 0
    while (new File(s"${config.storagePath}/$actorId-$eventNumber.bin").exists()) {
      eventNumber += 1
    }
    val fileName = s"${config.storagePath}/$actorId-$eventNumber.bin"
    logger.info(s"[LocalRecovery] Saving event for actorId: $actorId to file: $fileName")
    val oos      = new ObjectOutputStream(new FileOutputStream(fileName))
    oos.writeObject(event)
    oos.close()
  }

  override def delete(actorId: String): Unit = {
    logger.info(s"[LocalRecovery] Deleting all recovery files for actorId: $actorId")
    var eventNumber = 0
    var file        = new File(s"${config.storagePath}/$actorId-$eventNumber.bin")
    while (file.exists()) {
      logger.debug(s"[LocalRecovery] Deleting file: ${file.getPath}")
      file.delete()
      eventNumber += 1
      file = new File(s"${config.storagePath}/$actorId-$eventNumber.bin")
    }
  }

  override def doesFileExists(actorId: String): Boolean = {
    new File(s"${config.storagePath}/$actorId-0.bin").exists()
  }
}
