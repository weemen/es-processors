package actors

import org.apache.pekko.actor.typed.{Behavior, PostStop, Signal}
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import processors.BaseProcessor

object ProcessingActor {
  def apply(actorId: String, processorType: BaseProcessor): Behavior[Any] =
    Behaviors.setup[Any] { context =>
      val serviceKey = ServiceKey[Any](s"worker-$actorId")
      context.system.receptionist ! Receptionist.Register(serviceKey, context.self)
      new ProcessingActor(context, actorId, processorType)
    }
}

class ProcessingActor(context: ActorContext[Any], actorId: String, processorType: BaseProcessor)
    extends AbstractBehavior[Any](context) {
  context.log.info(s"ProcessorActor for ${processorType.getClass.getSimpleName} started with actorId: $actorId")

  override def onMessage(msg: Any): Behavior[Any] = {
    processorType.registerEvent(msg)
    val result: Option[Any] = processorType.process()
    result match {
      case Some(event) => context.log.info(s"Yielded event: $event")
      case None        => context.log.info(s"No event yielded")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Any]] = { case PostStop =>
    context.log.info(s"ProcessorActor with actorId: ${actorId} stopped")
    this
  }
}
