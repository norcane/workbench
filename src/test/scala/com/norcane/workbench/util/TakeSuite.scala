package com.norcane.workbench.util

import utest._

object TakeSuite extends TestSuite {

  override def tests = Tests {
    'testImmutableClass - {
      case class TestState(value: Int)

      val result = Take(TestState(0))
        .andThen(old => old.copy(old.value + 2))
        .andThen(old => old.copy(old.value + 3))
        .get.value

      assert(result == 5)
    }

    'testSideEffect - {
      class Mutable {
        var value: Int = 0
      }

      val result = Take(new Mutable)
        .sideEffect(_.value = 5)
        .get.value

      assert(result == 5)
    }
  }
}
