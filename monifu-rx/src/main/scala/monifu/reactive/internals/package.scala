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
 
package monifu.reactive

import monifu.concurrent.Scheduler
import monifu.concurrent.atomic.AtomicBoolean
import monifu.reactive.Ack.{Cancel, Continue}

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

package object internals {
  /**
   * Internal extensions to Future[Ack] used in the implementation of Observable.
   */
  implicit class FutureAckExtensions(val source: Future[Ack]) extends AnyVal {
    /**
     * On Continue, triggers the execution of the given callback.
     */
    def onContinue(cb: => Unit)(implicit s: Scheduler): Unit =
      source match {
        case sync if sync.isCompleted =>
          if (sync == Continue || (sync != Cancel && sync.value.get == Continue.IsSuccess))
            try cb catch {
              case NonFatal(ex) =>
                s.reportFailure(ex)
            }
        case async =>
          async.onSuccess {
            case Continue => cb
          }
      }

    def onContinueComplete[T](observer: Observer[T], ex: Throwable = null)(implicit s: Scheduler): Unit =
      source match {
        case sync if sync.isCompleted =>
          if (sync == Continue || ((sync != Cancel) && sync.value.get == Continue.IsSuccess)) {
            var streamError = true
            try {
              if (ex eq null)
                observer.onComplete()
              else {
                streamError = false
                observer.onError(ex)
              }
            }
            catch {
              case NonFatal(err) =>
                if (streamError)
                  observer.onError(err)
                else
                  s.reportFailure(err)
            }
          }
        case async =>
          async.onSuccess {
            case Continue =>
              var streamError = true
              try {
                if (ex eq null)
                  observer.onComplete()
                else {
                  streamError = false
                  observer.onError(ex)
                }
              }
              catch {
                case NonFatal(err) =>
                  if (streamError)
                    observer.onError(err)
                  else
                    s.reportFailure(err)
              }
          }
      }

    def onContinueCompleteWith[T](observer: Observer[T], lastElem: T)(implicit s: Scheduler): Unit =
      source match {
        case sync if sync.isCompleted =>
          if (sync == Continue || ((sync != Cancel) && sync.value.get == Continue.IsSuccess)) {
            try {
              observer.onNext(lastElem)
              observer.onComplete()
            }
            catch {
              case NonFatal(err) =>
                observer.onError(err)
            }
          }
        case async =>
          async.onSuccess {
            case Continue =>
              try {
                observer.onNext(lastElem)
                observer.onComplete()
              }
              catch {
                case NonFatal(err) =>
                  observer.onError(err)
              }
          }
      }

    /**
     * On Cancel, triggers Continue on the given Promise.
     */
    def onCancelContinue(p: Promise[Ack])(implicit s: Scheduler): Future[Ack] = {
      source match {
        case Continue => // do nothing
        case Cancel => p.success(Continue)

        case sync if sync.isCompleted && sync.value.get.isSuccess =>
          sync.value.get.get match {
            case Continue => // do nothing
            case Cancel => p.success(Continue)
          }

        case async =>
          async.onComplete {
            case Continue.IsSuccess => // nothing
            case Cancel.IsSuccess => p.success(Continue)
            case Failure(ex) => p.failure(ex)
            case other =>
              // branch not necessary, but Scala's compiler emits warnings if missing
              s.reportFailure(new MatchError(other.toString))
          }
      }
      source
    }

    /**
     * On Cancel, try to trigger Cancel on the given Promise.
     */
    def ifCancelTryCanceling(p: Promise[Ack])(implicit s: Scheduler): Future[Ack] = {
      source match {
        case Continue => // do nothing
        case Cancel => p.trySuccess(Cancel)

        case sync if sync.isCompleted =>
          sync.value.get match {
            case Continue.IsSuccess => // do nothing
            case Cancel.IsSuccess => p.trySuccess(Cancel)
            case Failure(ex) => p.tryFailure(ex)
            case other =>
              // branch not necessary, but Scala's compiler emits warnings if missing
              s.reportFailure(new MatchError(other.toString))
          }

        case async =>
          async.onComplete {
            case Continue.IsSuccess => // nothing
            case Cancel.IsSuccess => p.trySuccess(Cancel)
            case Failure(ex) => p.tryFailure(ex)
            case other =>
              // branch not necessary, but Scala's compiler emits warnings if missing
              s.reportFailure(new MatchError(other.toString))
          }
      }
      source
    }

    /**
     * On Cancel, try to trigger Cancel on the given Promise.
     */
    def ifCanceledDoCancel(p: Promise[Ack])(implicit s: Scheduler): Future[Ack] = {
      source match {
        case Continue => // do nothing
        case Cancel => p.success(Cancel)

        case sync if sync.isCompleted =>
          sync.value.get match {
            case Continue.IsSuccess => // do nothing
            case Cancel.IsSuccess => p.success(Cancel)
            case Failure(ex) => p.failure(ex)
            case other =>
              // branch not necessary, but Scala's compiler emits warnings if missing
              s.reportFailure(new MatchError(other.toString))
          }

        case async =>
          async.onComplete {
            case Continue.IsSuccess => // nothing
            case Cancel.IsSuccess => p.success(Cancel)
            case Failure(ex) => p.failure(ex)
            case other =>
              // branch not necessary, but Scala's compiler emits warnings if missing
              s.reportFailure(new MatchError(other.toString))
          }
      }
      source
    }

    /**
     * Unsafe version of `onComplete` that triggers execution synchronously
     * in case the source is already completed.
     */
    def onCompleteNow(f: Try[Ack] => Unit)(implicit s: Scheduler): Future[Ack] =
      source match {
        case sync if sync.isCompleted =>
          try f(sync.value.get) catch {
            case NonFatal(ex) =>
              s.reportFailure(ex)
          }
          source
        case async =>
          source.onComplete(f)
          source
      }

    /**
     * Triggers execution of the given callback, once the source terminates either
     * with a `Cancel` or with a failure.
     */
    def onCancel(cb: => Unit)(implicit s: Scheduler): Future[Ack] =
      source match {
        case Continue => source
        case Cancel =>
          try cb catch { case NonFatal(ex) => s.reportFailure(ex) }
          source
        case sync if sync.isCompleted =>
          sync.value.get match {
            case Continue.IsSuccess => source
            case Cancel.IsSuccess | Failure(_) =>
              try cb catch { case NonFatal(ex) => s.reportFailure(ex) }
              source
            case other =>
              // branch not necessary, but Scala's compiler emits warnings if missing
              s.reportFailure(new MatchError(other.toString))
              source
          }
        case async =>
          source.onComplete {
            case Cancel.IsSuccess | Failure(_) => cb
            case _ => // nothing
          }
          source
      }

    /**
     * Utility used in [[monifu.reactive.observers.SafeObserver.onNext]] for
     * handling errors in `onNext`. Avoids submitting tasks to the pool in case the
     * future is already complete and we can thus determine if it's a failure,
     * in which case we only need to cancel the stream and report the error.
     */
    def onErrorCancelStream[T](downstream: Observer[T], isDone: AtomicBoolean)
        (implicit scheduler: Scheduler): Future[Ack] = {

      def report(ex: Throwable) = {
        if (isDone.compareAndSet(expect=false, update=true)) {
          try downstream.onError(ex) catch {
            case NonFatal(oops) =>
              scheduler.reportFailure(oops)
          }

          Cancel
        }
        else {
          scheduler.reportFailure(ex)
          Cancel
        }
      }

      source match {
        case sync if sync.isCompleted =>
          if (sync.value.get.isFailure)
            report(sync.value.get.failed.get)
          else
            sync

        case async =>
          source.recover { case ex => report(ex) }
      }
    }
  }
}
