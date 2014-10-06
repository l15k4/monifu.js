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
import monifu.concurrent.cancelables.RefCountCancelable
import monifu.reactive.Ack.{Cancel, Continue}
import monifu.reactive.{Observable, Ack, Observer}
import monifu.reactive.internals._
import scala.concurrent.{Promise, Future}
import scala.util.control.NonFatal

object flatScan {
  /**
   * Implementation for [[Observable.flatScan]].
   */
  def apply[T,R](source: Observable[T], initial: R)(op: (R, T) => Observable[R])(implicit s: Scheduler) =
    Observable.create[R] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        private[this] val refCount = RefCountCancelable(observer.onComplete())
        private[this] var state = initial

        def onNext(elem: T): Future[Ack] = {
          val refID = refCount.acquire()
          val upstreamPromise = Promise[Ack]()
          var streamError = true

          try {
            val newState = op(state, elem)
            streamError = false

            newState.unsafeSubscribe(new Observer[R] {
              private[this] var ack = Continue : Future[Ack]

              def onNext(elem: R): Future[Ack] = {
                state = elem
                ack = observer.onNext(elem)
                  .ifCancelTryCanceling(upstreamPromise)
                ack
              }

              def onError(ex: Throwable): Unit = {
                if (upstreamPromise.trySuccess(Cancel))
                  observer.onError(ex)
              }

              def onComplete(): Unit =
                ack.onContinue {
                  refID.cancel()
                  upstreamPromise.trySuccess(Continue)
                }
            })
          }
          catch {
            case NonFatal(ex) =>
              if (streamError) {
                observer.onError(ex)
                Cancel
              }
              else
                Future.failed(ex)
          }

          upstreamPromise.future
        }

        def onComplete() = {
          refCount.cancel()
        }

        def onError(ex: Throwable) = {
          observer.onError(ex)
        }
      })
    }
}
