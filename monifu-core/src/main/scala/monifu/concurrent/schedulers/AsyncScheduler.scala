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
 
package monifu.concurrent.schedulers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import monifu.concurrent.{Cancelable, Scheduler}

private[concurrent] object AsyncScheduler extends Scheduler {
  def scheduleOnce(initialDelay: FiniteDuration, action: => Unit)(implicit ec: ExecutionContext): Cancelable = {
    val task = setTimeout(initialDelay.toMillis, {
      ec.execute(new Runnable { def run() = action })
    })
    Cancelable(clearTimeout(task))
  }

  private[this] def setTimeout(delayMillis: Long, cb: => Unit)(implicit ec: ExecutionContext): js.Dynamic = {
    val lambda: js.Function = () =>
      try { cb } catch { case t: Throwable => ec.reportFailure(t) }
    js.Dynamic.global.setTimeout(lambda, delayMillis)
  }

  private[this] def clearTimeout(task: js.Dynamic) = {
    js.Dynamic.global.clearTimeout(task)
  }
}
