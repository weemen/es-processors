package storage

import actors.CborSerializable

trait Recovery {

  def recover(actorId: String): List[CborSerializable]
  def save(actorId: String, event: CborSerializable): Unit
  def delete(actorId: String): Unit
  def doesFileExists(actorId: String): Boolean
}
