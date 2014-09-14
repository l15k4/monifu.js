package monifu.concurrent

import scala.concurrent.ExecutionContext

object Implicits {
  implicit val scheduler: Scheduler =
    Scheduler.global

  implicit val executionContext: ExecutionContext =
    DefaultExecutionContext
}
