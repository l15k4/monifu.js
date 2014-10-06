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

object JSSetTest extends JasmineTest {
  case class Boxed[T](value: T)

  case class Tuple[T](value1: T, value2: T) {
    override def hashCode() = {
      value1.hashCode()
    }
  }

  describe("JSSet") {
    it("should add") {
      val set = JSSet.empty[Boxed[Int]]

      expect(set.add(Boxed(1))).toBe(true)
      expect(set.add(Boxed(2))).toBe(true)
      expect(set.add(Boxed(3))).toBe(true)

      expect(set.add(Boxed(1))).toBe(false)
      expect(set.add(Boxed(2))).toBe(false)
      expect(set.add(Boxed(3))).toBe(false)
    }

    it("should remove") {
      val set = JSSet(Boxed(1), Boxed(2), Boxed(3))

      expect(set.remove(Boxed(1))).toBe(true)
      expect(set.remove(Boxed(2))).toBe(true)
      expect(set.remove(Boxed(3))).toBe(true)

      expect(set.remove(Boxed(1))).toBe(false)
      expect(set.remove(Boxed(2))).toBe(false)
      expect(set.remove(Boxed(3))).toBe(false)
    }

    it("should do contains") {
      val set = JSSet(Boxed(1), Boxed(2), Boxed(3))

      expect(set.contains(Boxed(1))).toBe(true)
      expect(set.contains(Boxed(2))).toBe(true)
      expect(set.contains(Boxed(3))).toBe(true)

      expect(set.contains(Boxed(4))).toBe(false)
      expect(set.contains(Boxed(5))).toBe(false)
      expect(set.contains(Boxed(6))).toBe(false)
    }

    it("should count size") {
      val set = JSSet(Boxed(1), Boxed(2), Boxed(3))
      expect(set.size).toBe(3)

      set.add(Boxed(4))
      expect(set.size).toBe(4)
      set.add(Boxed(1))
      expect(set.size).toBe(4)

      set.remove(Boxed(1))
      expect(set.size).toBe(3)
      set.clear()
      expect(set.size).toBe(0)
    }

    it("should work with collisions") {
      val set = JSSet(Tuple(1,1), Tuple(1,2), Tuple(1,3))
      expect(set.size).toBe(3)

      expect(set.remove(Tuple(1,1))).toBe(true)
      expect(set.remove(Tuple(1,1))).toBe(false)
      expect(set.size).toBe(2)

      expect(set.remove(Tuple(1,2))).toBe(true)
      expect(set.remove(Tuple(1,2))).toBe(false)
      expect(set.size).toBe(1)

      expect(set.remove(Tuple(1,3))).toBe(true)
      expect(set.remove(Tuple(1,3))).toBe(false)
      expect(set.size).toBe(0)

      expect(set.add(Tuple(1,1))).toBe(true)
      expect(set.add(Tuple(1,1))).toBe(false)
      expect(set.size).toBe(1)

      expect(set.add(Tuple(1,2))).toBe(true)
      expect(set.add(Tuple(1,2))).toBe(false)
      expect(set.size).toBe(2)
    }
  }
}
