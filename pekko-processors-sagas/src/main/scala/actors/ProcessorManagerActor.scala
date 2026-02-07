package actors

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop, Props, Signal, SpawnProtocol}
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.util.Timeout
import processors.BaseProcessor

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.*

final case class RegisterProcessor(processor: BaseProcessor)
final case class ProcessEvent[A](event: A, identifier: String)

object ProcessorManagerActor {
  def apply(actorId: String): Behavior[Any] =
    Behaviors.setup[Any] { context =>
      val serviceKey = ServiceKey[Any](s"manager-$actorId")
      context.system.receptionist ! Receptionist.Register(serviceKey, context.self)
      new ProcessorManagerActor(actorId)(using context)
    }
}

class ProcessorManagerActor(actorId: String)(using context: ActorContext[Any]) extends AbstractBehavior[Any](context) {
  context.log.info(s"ProcessorManagerActor for started with actorId: $actorId")

  implicit val timeout: Timeout                                  = 3.seconds
  implicit val scheduler: org.apache.pekko.actor.typed.Scheduler = context.system.scheduler

  private val registeredProcessors: ListBuffer[BaseProcessor] = ListBuffer.empty

  override def onMessage(msg: Any): Behavior[Any] = {
    msg match {
      case processor: RegisterProcessor    =>
        context.log.info(
          s"[ProcessorManagerActor] Registering processor: ${processor.processor.getClass.getSimpleName}"
        )
        registeredProcessors.addOne(processor.processor)
      case ProcessEvent(event, identifier) => broadcastEvent(event, identifier)
      case _                               => context.log.info(s"[ProcessorManagerActor] I cannot handler this message: ${msg.toString}")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[Any]] = { case PostStop =>
    context.log.info(s"ProcessorManagerActor with actorId: ${actorId} stopped")
    this
  }

  private def getClassName(obj: Any): String = {
    val name = obj match {
      case c: Class[_] => c.getSimpleName
      case _           => obj.getClass.getSimpleName
    }
    if (name.endsWith("$")) name.init else name
  }

  private def broadcastEvent(event: Any, identifier: String): Unit = {
    val msgInstanceType = event.getClass.getSimpleName
    registeredProcessors.foreach(processor => {
      val requiredEventNames = processor.listOfEvents.map(getClassName)
      if (requiredEventNames.contains(msgInstanceType)) {
        sendEventToProcessor(event, identifier, processor)
      } else {
        context.log.info(s"Event $msgInstanceType not found in ${processor.getClass.getSimpleName}")
      }
    })
  }

  private def sendEventToProcessor(event: Any, identifier: String, processor: BaseProcessor): Unit = {
    getOrCreateActor(context.system, identifier, processor).tell(event)
  }

  def getActorByActorId(system: ActorSystem[?], actorId: String)(implicit
      timeout: Timeout,
      scheduler: org.apache.pekko.actor.typed.Scheduler
  ): Option[ActorRef[Any]] = {
    val serviceKey = ServiceKey[Any](s"worker-$actorId")
    val future     = system.receptionist.ask[Receptionist.Listing](replyTo => Receptionist.Find(serviceKey, replyTo))
    val listing    = Await.result(future, timeout.duration)
    listing.serviceInstances(serviceKey).headOption
  }

  def getOrCreateActor(system: ActorSystem[?], actorId: String, processor: BaseProcessor)(implicit
      timeout: Timeout,
      scheduler: org.apache.pekko.actor.typed.Scheduler
  ): ActorRef[Any] = {
    context.child(actorId) match {
      case Some(ref) => ref.asInstanceOf[ActorRef[Any]]
      case None =>
        getActorByActorId(system, actorId).getOrElse {
          context.log.info(s"Actor with ID: $actorId not found, spawning new one.")
          spawnActorForBaseProcessor(system, actorId, processor)
        }
    }
  }

  def spawnActorForBaseProcessor(
      system: ActorSystem[?],
      actorId: String,
      processor: BaseProcessor
  )(implicit timeout: Timeout, scheduler: org.apache.pekko.actor.typed.Scheduler): ActorRef[Any] = {
    // We try to spawn via the actor context first, which is more standard for child actors.
    // The previous implementation tried to use SpawnProtocol on the system, which is only needed if spawning from outside.
    context.child(actorId) match {
      case Some(ref) => ref.asInstanceOf[ActorRef[Any]]
      case None => context.spawn(ProcessingActor(actorId, processor), actorId)
    }
  }
}
