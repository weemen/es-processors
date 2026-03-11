package actors

import com.typesafe.config.ConfigFactory
import config.StorageConfig
import org.apache.pekko.actor.typed.{Behavior, PostStop, PreRestart, Signal}
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import processors.BaseProcessor
import storage.{LocalRecovery, Recovery}

object ProcessingActor {
  def apply(actorId: String, processorType: BaseProcessor): Behavior[Any] =
    Behaviors.setup[Any] { context =>
      val serviceKey = ServiceKey[Any](s"worker-$actorId")
      context.system.receptionist ! Receptionist.Register(serviceKey, context.self)

      // the config needs to be here.
      val config   = ConfigFactory.load()
      val strategy = config.getConfig("es-processors-sagas.recovery.strategy").getString("type")

      val recovery = strategy match {
        case "local" =>
          LocalRecovery(
            StorageConfig.Local(
              config.getConfig("es-processors-sagas.recovery.strategy.local").getString("storage_path")
            )
          )
        case _       => throw new Exception("Strategy not supported")
      }
      new ProcessingActor(actorId, processorType, recovery: Recovery)(using context)
    }
}

class ProcessingActor(actorId: String, processorType: BaseProcessor, recovery: Recovery)(using
    context: ActorContext[Any]
) extends AbstractBehavior[Any](context) {
  context.log.info(s"ProcessorActor for ${processorType.getClass.getSimpleName} (re)started with actorId: $actorId")

  override def onMessage(msg: Any): Behavior[Any] = {
    msg match {
      case event: CborSerializable =>
        recovery.save(actorId = actorId, event = event)
        processorType.registerEvent(msg)
        val result: Option[Any] = processorType.process()
        result match {
          case Some(event) =>
            context.log.info(s"Yielded event: $event")
            recovery.delete(actorId = actorId)
          case None        => context.log.info(s"No event yielded")
        }
      case _                       =>
        context.log.debug(s"Received message that is not CborSerializable: $msg")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Any]] = {
    case PostStop   =>
      context.log.info(s"ProcessorActor with actorId: ${actorId} stopped")
      this
    case PreRestart =>
      // reload events from recovery storage
      recovery.recover(actorId = actorId).foreach(event => onMessage(event))
      context.log.info(s"ProcessorActor with actorId: ${actorId} is about to restart")
      this
  }
}
