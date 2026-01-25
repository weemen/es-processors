package weemen
package processors

/**
 * SimpleESProcessor ensures that handleEventFn takes exactly one input parameter of type A.
 * In Scala, A => Boolean is a Function1[A, Boolean], which inherently enforces this.
 */
class SimpleESProcessor[A](val handleEventFn : A => Boolean) {

}
