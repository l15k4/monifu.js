/*
 * Copyright (c) 2014 by its authors. Some rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monifu.concurrent

import scala.concurrent.{ExecutionContext, Promise}
import scala.scalajs.test.JasmineTest


object TrampolinedExecutionContextTest extends JasmineTest {
  val ec = TrampolinedExecutionContext(new ExecutionContext {
    def execute(runnable: Runnable) = {
      monifu.concurrent.Implicits
        .executionContext.execute(runnable)
    }

    def reportFailure(ex: Throwable) = {
      if (!ex.getMessage.contains("test-exception"))
        throw ex
    }
  })

  def runnable(action: => Unit) =
    new Runnable { def run() = action }

  describe("PossiblyImmediateScheduler") {
    beforeEach {
      jasmine.Clock.useMock()
    }

    it("should scheduleOnce") {
      val p = Promise[Int]()
      ec.execute(runnable { p.success(1) })
      val f = p.future

      jasmine.Clock.tick(1)
      expect(f.value.get.get).toBe(1)
    }

    it("should execute async") {
      var stackDepth = 0
      var iterations = 0

      ec.execute(runnable {
        stackDepth += 1
        iterations += 1
        ec.execute(runnable {
          stackDepth += 1
          iterations += 1
          expect(stackDepth).toBe(1)

          ec.execute(runnable {
            stackDepth += 1
            iterations += 1
            expect(stackDepth).toBe(1)
            stackDepth -= 1
          })

          expect(stackDepth).toBe(1)
          stackDepth -= 1
        })

        expect(stackDepth).toBe(1)
        stackDepth -= 1
      })

      expect(iterations).toBe(3)
      expect(stackDepth).toBe(0)
    }

    it("should not trigger a stack overflow") {
      var effect = 0
      def loop(until: Int): Unit =
        if (effect < until)
          ec.execute(runnable {
            effect += 1
            loop(until)
          })

      loop(until = 100000)
      jasmine.Clock.tick(1)

      expect(effect).toBe(100000)
    }
  }
}
