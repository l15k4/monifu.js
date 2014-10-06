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

import scala.collection.generic.{GenericTraversableTemplate, SeqFactory}
import scala.collection.mutable
import scala.scalajs.js

/**
 * An efficient mutable queue implementation.
 *
 * Javascript Arrays have an efficient append operation in the
 * form of `.push` and this implementation piggybacks on this functionality. 
 * On the other hand the `array.shift()` operation is inefficient. This
 * implementation runs `dequeue` in amortized constant time. It is
 * also more efficient than what provided by Scala because it directly
 * takes advantage of array operations as available in Javascript.
 *
 * Inspired by: http://code.stephenmorley.org/javascript/queues/
 */
final class JSArrayQueue[T](items: T*)
  extends mutable.Seq[T]
  with mutable.Cloneable[JSArrayQueue[T]]
  with GenericTraversableTemplate[T, JSArrayQueue]
  with Serializable
{
  private[this] var array = js.Array[T](items : _*)
  private[this] var offset = 0

  override def companion = {
    JSArrayQueue
  }

  def enqueue(elems: T*): Unit = {
    array.push(elems : _*)
  }

  def dequeue(): T = {
    if (size == 0) throw new NoSuchElementException("queue empty")

    val dequeued = array(offset)
    offset += 1

    if (offset > 0 && offset >= array.length / 2) {
      // get rid of garbage
      array = array.slice(offset)
      offset = 0
    }

    dequeued
  }

  override def update(idx: Int, elem: T): Unit = {
    if (idx < 0 || idx + offset >= array.length)
      throw new NoSuchElementException(s"Index $idx does not exist")
    array(offset + idx) = elem
  }

  override def length: Int = {
    array.length - offset
  }

  override def size: Int = {
    array.length - offset
  }

  override def isEmpty: Boolean = {
    size == 0
  }

  override def nonEmpty: Boolean = {
    size != 0
  }

  override def apply(idx: Int): T = {
    if (idx < 0 || idx + offset >= array.length)
      throw new NoSuchElementException(s"Index $idx does not exist")

    array(offset + idx)
  }

  override def iterator: Iterator[T] =
    new Iterator[T] {
      private[this] var idx = offset

      def hasNext: Boolean = {
        idx < array.length
      }

      def next(): T = {
        val item = array(idx)
        idx += 1
        item
      }
    }

  override def seq: mutable.Seq[T] = {
    this
  }

  override def clone(): JSArrayQueue[T] = {
    new JSArrayQueue[T](array.slice(offset) : _*)
  }

  override def drop(n: Int): this.type = {
    var idx = 0
    while (idx < n && nonEmpty) {
      dequeue()
      idx += 1
    }
    this
  }

  override def dropWhile(p: (T) => Boolean): this.type = {
    var continue = true
    while (continue && nonEmpty) {
      continue = p(array(offset))
      if (continue) dequeue()
    }
    this
  }

  override def head: T = {
    if (isEmpty) throw new NoSuchElementException("JSArrayQueue.head")
    array(offset)
  }

  override def tail: mutable.Seq[T] = {
    val newArray = array.slice(offset + 1)
    new JSArrayQueue[T](newArray : _*)
  }

  def clear(): Unit = {
    array = js.Array[T]()
    offset = 0
  }
}


/**
 * An efficient mutable queue implementation.
 *
 * Javascript Arrays have an efficient append operation in the
 * form of `.push` and this implementation piggybacks on this functionality.
 * On the other hand the `array.shift()` operation is inefficient. This
 * implementation runs `dequeue` in amortized constant time. It is
 * also more efficient than what provided by Scala because it directly
 * takes advantage of array operations as available in Javascript.
 *
 * Inspired by: http://code.stephenmorley.org/javascript/queues/
 */
object JSArrayQueue extends SeqFactory[JSArrayQueue] {
  def newBuilder[A] = new mutable.Builder[A, JSArrayQueue[A]] {
    private[this] var queue = new JSArrayQueue[A]()

    def +=(elem: A): this.type = {
      queue.enqueue(elem)
      this
    }

    def result(): JSArrayQueue[A] = {
      queue
    }

    def clear(): Unit = {
      queue = new JSArrayQueue[A]()
    }
  }
}
