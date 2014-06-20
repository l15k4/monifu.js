/*
 * Copyright (c) 2014 by its authors. Some rights reserved. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package monifu.concurrent

import language.experimental.macros
import scala.util.Try
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.concurrent.duration._
import java.util.concurrent.TimeoutException


object extensions {
  /**
   * Provides utility methods added on Scala's `concurrent.Future`
   */
  implicit class FutureExtensions[T](val source: Future[T]) extends AnyVal {
    /**
     * Combinator that returns a new Future that either completes with
     * the original Future's result or with a TimeoutException in case
     * the maximum wait time was exceeded.
     *
     * @param atMost specifies the maximum wait time until the future is
     *               terminated with a TimeoutException
     * @param s is the implicit Scheduler, needed for completing our internal promise
     */
    def withTimeout(atMost: FiniteDuration)(implicit s: Scheduler): Future[T] = {
      // catching the exception here, for non-useless stack traces
      val err = Try(throw new TimeoutException)
      val promise = Promise[T]()
      val task = s.scheduleOnce(atMost, promise.tryComplete(err))

      source.onComplete { case r =>
        // canceling task to prevent waisted CPU resources and memory leaks
        // if the task has been executed already, this has no effect
        task.cancel()
        promise.tryComplete(r)
      }

      promise.future
    }

    /**
     * Utility that lifts a `Future[T]` into a `Future[Try[T]]`, just because
     * it is useful sometimes.
     */
    def liftTry(implicit ec: ExecutionContext): Future[Try[T]] = {
      if (source.isCompleted) {
        Future.successful(source.value.get)
      }
      else {
        val p = Promise[Try[T]]()
        source.onComplete { case result => p.success(result) }
        p.future
      }
    }

    /**
     * Returns a new `Future` that takes `atLeast` and `atMost` time to execute.
     *
     * @param atLeast the minimal duration that the returned future will take to complete.
     * @param atMost the maximum duration that the returned future will take to complete (otherwise it gets completed with a `TimeoutException`)
     * @param s the implicit scheduler that handles the time scheduling
     * @return a new `Future` whose execution time is within the specified bounds
     */
    def ensureDuration(atLeast: FiniteDuration, atMost: Duration = Duration.Inf)(implicit s: Scheduler): Future[T] = {
      require(atMost == Duration.Inf || atMost > atLeast)

      val start = System.nanoTime()
      val future = if (atMost.isFinite()) source.withTimeout(atMost.asInstanceOf[FiniteDuration]) else source
      val p = Promise[T]()

      future.onComplete {
        case result =>
          val remainingNanos = atLeast.toNanos - (System.nanoTime() - start)
          if (remainingNanos >= 1000000) {
            val remaining = if (remainingNanos % 1000000 == 0)
              (remainingNanos / 1000000).millis else ((remainingNanos / 1000000) + 1).millis
            s.scheduleOnce(remaining, p.complete(result))
          }
          else
            p.complete(result)
      }

      p.future
    }
  }

  /**
   * Provides utility methods for Scala's `concurrent.Future` companion object.
   */
  implicit class FutureCompanionExtensions(val f: Future.type) extends AnyVal {
    /**
     * Future that completes with the specified `result`, but only
     * after the specified `delay`.
     */
    def delayedResult[T](delay: FiniteDuration)(result: => T)(implicit s: Scheduler): Future[T] = {
      val p = Promise[T]()
      s.scheduleOnce(delay, p.success(result))
      p.future
    }
  }
}
