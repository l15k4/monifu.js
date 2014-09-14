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

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
 * An execution context that runs scheduled tasks synchronously.
 *
 * @param fallback is the fallback `ExecutionContext` used for rescheduling
 *                 pending tasks in case the currently running task either
 *                 triggers an error or is executing a blocking task.
 */
final class TrampolinedExecutionContext private (fallback: ExecutionContext)
  extends ExecutionContext {

  private[this] val queue = mutable.Queue.empty[Runnable]
  private[this] var withinLoop = false

  def execute(runnable: Runnable): Unit = {
    queue.enqueue(runnable)
    if (!withinLoop) {
      withinLoop = true
      try  { immediateLoop() } finally { withinLoop = false }
    }
  }

  @tailrec
  private[this] def immediateLoop(): Unit = {
    if (queue.nonEmpty) {
      val task = queue.dequeue()

      try {
        task.run()
      }
      catch {
        case NonFatal(ex) =>
          // exception in the immediate scheduler must be reported
          // but first reschedule the pending tasks on the fallback
          try { rescheduleOnFallback() } finally {
            reportFailure(ex)
          }
      }

      immediateLoop()
    }
  }

  @tailrec
  private[this] def rescheduleOnFallback(): Unit =
    if (queue.nonEmpty) {
      val task = queue.dequeue()
      fallback.execute(task)
      rescheduleOnFallback()
    }

  def reportFailure(t: Throwable): Unit =
    fallback.reportFailure(t)
}


object TrampolinedExecutionContext {
  def apply(fallback: ExecutionContext): ExecutionContext =
    new TrampolinedExecutionContext(fallback)

  object Implicits {
    implicit lazy val executionContext: ExecutionContext =
      apply(DefaultExecutionContext)
  }
}
