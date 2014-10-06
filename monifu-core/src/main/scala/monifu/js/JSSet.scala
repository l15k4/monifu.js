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

import scala.annotation.tailrec
import scala.scalajs.js

/**
 * An efficient mutable set implementation, based on Javascript dictionaries, with
 * plain arrays to handle the collisions.
 */
final class JSSet[T](initialElems: T*) extends scala.collection.mutable.Set[T] {
  private[this] var count = 0
  private[this] var dictionary = js.Dictionary[js.Array[T]]()

  locally {
    for (e <- initialElems)
      this += e
  }

  def contains(elem: T): Boolean = {
    if (count == 0) false
    else {
      val hashCode = elem.hashCode().toString
      if (!dictionary.contains(hashCode))
        false
      else
        arrayContains(dictionary(hashCode), elem)
    }
  }

  def +=(elem: T) = {
    add(elem)
    this
  }

  override def add(elem: T): Boolean = {
    val hashCode = elem.hashCode().toString
    if (!dictionary.contains(hashCode)) {
      val newArray = js.Array[T](elem)
      dictionary.update(hashCode, newArray)
      count += 1
      true
    }
    else {
      val array = dictionary(hashCode)
      if (!arrayContains(array, elem)) {
        array.push(elem)
        count += 1
        true
      }
      else
        false
    }
  }

  def -=(elem: T) = {
    remove(elem)
    this
  }

  override def remove(elem: T): Boolean = {
    val hashCode = elem.hashCode().toString
    if (dictionary.contains(hashCode)) {
      val array = dictionary(hashCode)
      if (array.length == 0) {
        false
      }
      else if (array.length == 1) {
        if (array(0) == elem) {
          dictionary.remove(hashCode)
          count -= 1
          true
        }
        else
          false
      }
      else {
        val index = arrayIndexOf(array, elem)
        if (index >= 0) {
          // copying elements to new array
          val newArray = new js.Array[T](array.length - 1)
          var i = 0

          while (i < array.length) {
            if (i < index)
              newArray(i) = array(i)
            else if (i > index)
              newArray(i - 1) = array(i)

            i += 1
          }

          dictionary.update(hashCode, newArray)
          count -= 1
          true
        }
        else
          false
      }
    }
    else
      false
  }

  override def size = {
    count
  }

  override def isEmpty = {
    count == 0
  }


  override def nonEmpty = {
    count != 0
  }

  override def clear() = {
    dictionary = js.Dictionary[js.Array[T]]()
    count = 0
  }

  def iterator = new Iterator[T] {
    private[this] val dictionaryIterator =
      dictionary.iterator

    private[this] var arrayIterator: Iterator[T] =
      null

    @tailrec
    def hasNext = {
      val hasFromArray = arrayIterator != null && arrayIterator.hasNext
      if (!hasFromArray)
        if (dictionaryIterator.hasNext) {
          arrayIterator = dictionaryIterator.next()._2.iterator
          hasNext
        }
        else
          false
      else
        true
    }

    def next() = {
      assert(arrayIterator != null,
        "Incorrect usage of the Iterator interface, either hasNext hasn't been called, " +
        "or iterator is empty")
      arrayIterator.next()
    }
  }

  private[this] def arrayIndexOf(array: js.Array[T], elem: T): Int = {
    var elemFirstIndex = -1
    var idx = 0
    while (elemFirstIndex == -1 && idx < array.length) {
      if (array(idx) == elem) elemFirstIndex = idx
      else idx += 1
    }
    elemFirstIndex
  }

  private[this] def arrayContains(array: js.Array[T], elem: T): Boolean = {
    var found = false
    var idx = 0
    while (!found && idx < array.length) {
      if (array(idx) == elem) found = true
      else idx += 1
    }
    found
  }
}

/**
 * An efficient mutable set implementation, based on Javascript dictionaries, with
 * plain arrays to handle the collisions.
 */
object JSSet {
  def empty[T]: JSSet[T] = new JSSet[T]()

  def apply[T](elems: T*): JSSet[T] =
    new JSSet[T](elems: _*)
}