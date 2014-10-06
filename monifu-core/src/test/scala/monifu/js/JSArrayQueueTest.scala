/*
 * Copyright (c) 2014 by its authors. Some rights reserved.
 * See the project homepage at
 *
 *     http://www.monifu.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monifu.js

import scala.scalajs.test.JasmineTest

object JSArrayQueueTest extends JasmineTest {
  describe("Queue") {
    it("should work") {
      val count = 1000
      val queue = JSArrayQueue.empty[Int]

      for (i <- 0 until count)
        queue.enqueue(i)

      for (idx <- 0 until count) {
        expect(queue.dequeue()).toBe(idx)
        expect(queue.length).toBe(count - (idx + 1))
      }
    }

    it("should iterate") {
      val count = 1000
      val queue = JSArrayQueue.empty[Int]

      for (i <- 0 until count / 2)
        queue.enqueue(i)
      for (i <- 0 until count / 2)
        expect(queue.dequeue()).toBe(i)
      for (i <- 0 until count)
        queue.enqueue(i)

      var sum = 0
      for (elem <- queue)
        sum += elem

      expect(sum).toBe(count / 2 * (count - 1))
    }
  }
}
