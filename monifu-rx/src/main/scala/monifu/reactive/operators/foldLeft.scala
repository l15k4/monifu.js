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

import monifu.reactive.Ack.{Cancel, Continue}
import monifu.reactive.{Observable, Ack, Observer}

import scala.concurrent.Future
import scala.util.control.NonFatal

object foldLeft {
  /**
   * Implementation for [[Observable.foldLeft]].
   */
  def apply[T, R](source: Observable[T], initial: R)(op: (R, T) => R) =
    Observable.create[R] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        private[this] var state = initial

        def onNext(elem: T): Future[Ack] = {
          // See Section 6.4. in the Rx Design Guidelines:
          // Protect calls to user code from within an operator
          try {
            state = op(state, elem)
            Continue
          }
          catch {
            case NonFatal(ex) =>
              onError(ex)
              Cancel
          }
        }

        def onComplete() = {
          observer.onNext(state)
          observer.onComplete()
        }

        def onError(ex: Throwable) = {
          observer.onError(ex)
        }
      })
    }
}
