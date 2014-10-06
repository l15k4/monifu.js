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

import monifu.reactive.Ack.Continue
import monifu.reactive.{Ack, Observer, Observable}

import scala.concurrent.Future

object misc {
  /**
   * Implements [[Observable.complete]].
   */
  def complete[T](source: Observable[T]) =
    Observable.create[Nothing] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        def onNext(elem: T) = Continue
        def onError(ex: Throwable): Unit =
          observer.onError(ex)
        def onComplete(): Unit =
          observer.onComplete()
      })
    }

  /**
   * Implements [[Observable.error]].
   */
  def error[T](source: Observable[T]) =
    Observable.create[Throwable] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        def onNext(elem: T) =
          Continue

        def onComplete(): Unit =
          observer.onComplete()

        def onError(ex: Throwable): Unit = {
          observer.onNext(ex)
          observer.onComplete()
        }
      })
    }

  /**
   * Implementation for [[monifu.reactive.Observable.defaultIfEmpty]].
   */
  def defaultIfEmpty[T](source: Observable[T], default: T) =
    Observable.create[T] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        private[this] var isEmpty = true

        def onNext(elem: T): Future[Ack] = {
          if (isEmpty) isEmpty = false
          observer.onNext(elem)
        }

        def onError(ex: Throwable): Unit = {
          observer.onError(ex)
        }

        def onComplete(): Unit = {
          if (isEmpty) observer.onNext(default)
          observer.onComplete()
        }
      })
    }

  /**
   * Implements [[Observable.endWithError]].
   */
  def endWithError[T](source: Observable[T])(error: Throwable) =
    Observable.create[T] { observer =>
      source.unsafeSubscribe(new Observer[T] {
        def onNext(elem: T) = observer.onNext(elem)
        def onError(ex: Throwable) = observer.onError(ex)
        def onComplete() = observer.onError(error)
      })
    }
}
