package storage

import actors.CborSerializable
import config.StorageConfig

import java.io.*

class LocalRecovery(config: StorageConfig.Local) extends Recovery {

  override def recover(actorId: String): List[CborSerializable] = {
    var events = List.empty[CborSerializable]
    var eventNumber = 0
    var keepGoing = true
    
    while (keepGoing) {
      val file = new File(s"${config.storagePath}/$actorId-$eventNumber.bin")
      if (file.exists()) {
        val ois = new ObjectInputStream(new FileInputStream(file))
        val event = ois.readObject().asInstanceOf[CborSerializable]
        events = events :+ event
        ois.close()
        eventNumber += 1
      } else {
        keepGoing = false
      }
    }
    events
  }

  override def save(actorId: String, event: CborSerializable): Unit = {
    var eventNumber = 0
    while (new File(s"${config.storagePath}/$actorId-$eventNumber.bin").exists()) {
      eventNumber += 1
    }
    val oos = new ObjectOutputStream(new FileOutputStream(s"${config.storagePath}/$actorId-$eventNumber.bin"))
    oos.writeObject(event)
    oos.close()
  }

  override def delete(actorId: String): Unit = {
    var eventNumber = 0
    var file = new File(s"${config.storagePath}/$actorId-$eventNumber.bin")
    while (file.exists()) {
      file.delete()
      eventNumber += 1
      file = new File(s"${config.storagePath}/$actorId-$eventNumber.bin")
    }
  }

  override def doesFileExists(actorId: String): Boolean = {
    new File(s"${config.storagePath}/$actorId-0.bin").exists()
  }
}
