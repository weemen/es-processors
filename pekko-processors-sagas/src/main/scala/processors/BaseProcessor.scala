package processors
import scala.collection.mutable.ListBuffer

import scala.reflect.ClassTag

abstract class BaseProcessor(val listOfEvents: List[Any]):
  protected val registeredEvents: ListBuffer[Any] = ListBuffer.empty

  private def getClassName(obj: Any): String =
    val name = obj match {
      case c: Class[_] => c.getSimpleName
      case _           => obj.getClass.getSimpleName
    }
    if (name.endsWith("$")) name.init else name

  def registerEvent[A](event: A): Unit =
    val eventName          = event.getClass.getSimpleName
    val requiredEventNames = listOfEvents.map(getClassName)
    if (requiredEventNames.contains(eventName) && !registeredEvents.exists(_.getClass.getSimpleName == eventName)) {
      registeredEvents += event
    }

  def getEventByType[T](using ct: ClassTag[T]): Option[T] =
    registeredEvents.collectFirst { case e: T => e }

  def process(): Option[Any]
