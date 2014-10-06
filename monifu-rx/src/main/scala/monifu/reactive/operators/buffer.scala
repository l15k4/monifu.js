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

package monifu.reactive.operators

import monifu.concurrent.Scheduler
import monifu.reactive.Ack.Continue
import monifu.reactive.internals._
import monifu.reactive.{Ack, Observable, Observer}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration


object buffer {
  /**
   * Implementation for [[Observable.buffer]].
   */
  def sized[T](source: Observable[T], count: Int)(implicit s: Scheduler): Observable[Seq[T]] =
    Observable.create { observer =>
      source.unsafeSubscribe(new Observer[T] {
        private[this] var buffer = ArrayBuffer.empty[T]
        private[this] var lastAck = Continue : Future[Ack]
        private[this] var size = 0

        def onNext(elem: T): Future[Ack] = {
          size += 1
          buffer.append(elem)
          if (size >= count) {
            val oldBuffer = buffer
            buffer = ArrayBuffer.empty[T]
            size = 0

            lastAck = observer.onNext(oldBuffer)
            lastAck
          }
          else
            Continue
        }

        def onError(ex: Throwable): Unit = {
          observer.onError(ex)
          buffer = null
        }

        def onComplete(): Unit = {
          if (size > 0) {
            // if we don't do this, then it breaks the
            // back-pressure contract
            lastAck.onContinueCompleteWith(observer, buffer)
          }
          else
            observer.onComplete()

          buffer = null
        }
      })
    }

  /**
   * Implementation for [[Observable.bufferTimed]].
   */
  def timed[T](source: Observable[T], timespan: FiniteDuration)(implicit s: Scheduler) =
    Observable.create[Seq[T]] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        private[this] val timespanMillis = timespan.toMillis
        private[this] var lastSignalTS = System.currentTimeMillis()
        private[this] var buffer = ArrayBuffer.empty[T]
        private[this] var lastAck: Future[Ack] = Continue

        def onNext(elem: T) = {
          val rightNow = System.currentTimeMillis()
          val expiresAt = lastSignalTS + timespanMillis
          buffer.append(elem)

          if (expiresAt <= rightNow) {
            lastAck = observer.onNext(buffer)
            buffer = ArrayBuffer.empty[T]
            lastSignalTS = rightNow
          }

          lastAck
        }

        def onError(ex: Throwable): Unit = {
          observer.onError(ex)
          buffer = null
        }

        def onComplete(): Unit = {
          if (buffer.nonEmpty) {
            // if we don't do this, then it breaks the
            // back-pressure contract
            lastAck.onContinueCompleteWith(observer, buffer)
          }
          else {
            observer.onComplete()
          }

          buffer = null
        }
      })
    }

  /**
   * Implementation for [[Observable.bufferSizedAndTimed]].
   */
  def sizedAndTimed[T](source: Observable[T], count: Int, timespan: FiniteDuration)(implicit s: Scheduler) =
    Observable.create[Seq[T]] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        private[this] val timespanMillis = timespan.toMillis
        private[this] var lastSignalTS = System.currentTimeMillis()
        private[this] var buffer = ArrayBuffer.empty[T]
        private[this] var lastAck: Future[Ack] = Continue

        def onNext(elem: T) = {
          val rightNow = System.currentTimeMillis()
          val expiresAt = lastSignalTS + timespanMillis
          buffer.append(elem)

          if (expiresAt <= rightNow || buffer.length >= count) {
            lastAck = observer.onNext(buffer)
            buffer = ArrayBuffer.empty[T]
            lastSignalTS = rightNow
          }

          lastAck
        }

        def onError(ex: Throwable): Unit = {
          observer.onError(ex)
          buffer = null
        }

        def onComplete(): Unit = {
          if (buffer.nonEmpty) {
            // if we don't do this, then it breaks the
            // back-pressure contract
            lastAck.onContinueCompleteWith(observer, buffer)
          }
          else {
            observer.onComplete()
          }

          buffer = null
        }
      })
    }
}
