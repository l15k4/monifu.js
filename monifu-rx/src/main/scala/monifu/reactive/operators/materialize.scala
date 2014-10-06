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
import monifu.reactive.Notification.{OnComplete, OnError, OnNext}
import monifu.reactive.{Notification, Observable, Ack, Observer}
import monifu.reactive.internals._
import scala.concurrent.Future


object materialize {
  /**
   * Implementation for [[Observable.materialize]].
   */
  def apply[T](source: Observable[T])(implicit s: Scheduler): Observable[Notification[T]] =
    Observable.create[Notification[T]] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        private[this] var ack = Continue : Future[Ack]

        def onNext(elem: T): Future[Ack] = {
          ack = observer.onNext(OnNext(elem))
          ack
        }

        def onError(ex: Throwable): Unit =
          ack.onContinue {
            observer.onNext(OnError(ex))
            observer.onComplete()
          }

        def onComplete(): Unit =
          ack.onContinue {
            observer.onNext(OnComplete)
            observer.onComplete()
          }
      })
    }
}
