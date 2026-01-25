package weemen
package processors

import org.scalatest.funsuite.AnyFunSuite

class SimpleESProcessorSpec extends AnyFunSuite {
  test("SimpleESProcessor should handle a single-parameter function") {
    val p1 = new SimpleESProcessor[Int](x => x > 0)
    assert(p1.handleEventFn(1) === true)
    assert(p1.handleEventFn(-1) === false)
  }

  test("SimpleESProcessor should handle a single-parameter function with a Tuple") {
    val p2 = new SimpleESProcessor[(Int, Int)](t => t._1 > t._2)
    assert(p2.handleEventFn((2, 1)) === true)
    assert(p2.handleEventFn((1, 2)) === false)
  }

  test("SimpleESProcessor should demonstrate enforcement of Function1") {
    // In Scala, A => Boolean is strictly Function1[A, Boolean].
    // The following would fail to compile:
    // val f2: (Int, Int) => Boolean = (x, y) => x > y
    // val p3 = new SimpleESProcessor[Int](f2) 
    
    val p1 = new SimpleESProcessor[Int](x => x > 10)
    assert(p1.handleEventFn.isInstanceOf[Function1[?, ?]])
  }
}
