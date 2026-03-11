package config

import cats.data.Reader

enum StorageConfig:
  case Local(storagePath: String)
  case Redis(username: String, password: String, host: String, port: Int)

final case class ProcessorActorConfig(strategy: String, config: StorageConfig)

object ProcessorActorConfig:
  val strategy: Reader[ProcessorActorConfig, String] = Reader(_.strategy)
  val config: Reader[ProcessorActorConfig, StorageConfig] = Reader(_.config)

