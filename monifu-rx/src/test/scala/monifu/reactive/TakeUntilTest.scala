package monifu.reactive

import monifu.reactive.Ack.Continue
import monifu.reactive.channels.PublishChannel
import monifu.reactive.subjects.PublishSubject

import scala.scalajs.test.JasmineTest
import scala.concurrent.duration._
import monifu.concurrent.Scheduler.Implicits.global


object TakeUntilTest extends JasmineTest {
  beforeEach {
    jasmine.Clock.useMock()
  }

  describe("Observable.takeUntil(other: Observable)") {
    it("should emit everything in case the other observable does not emit anything") {
      val other = Observable.never
      val f = Observable.from(0 until 1000)
        .delay(200.millis) // introducing artificial delay
        .takeUntil(other)
        .reduce(_ + _)
        .asFuture

      jasmine.Clock.tick(199)
      expect(f.isCompleted).toBe(false)
      jasmine.Clock.tick(1)
      expect(f.isCompleted).toBe(true)
      expect(f.value.get.get.get).toBe(500 * 999)
    }

    it("should stop in case the other observable signals onNext") {
      val trigger = PublishChannel[Unit]()
      val channel = PublishSubject[Int]()
      var sum = 0

      channel.takeUntil(trigger).subscribe(
        elem => { sum += elem; Continue },
        ex => throw ex
      )

      for (_ <- 0 until 1000) {
        val ack = channel.onNext(1)
        expect(ack.value.get.get.toString).toBe("Continue")
      }

      trigger.pushNext(())
      jasmine.Clock.tick(1)
      expect(sum).toBe(1000)
    }

    it("should stop in case the other observable signals onComplete") {
      val trigger = PublishChannel[Unit]()
      val channel = PublishSubject[Int]()
      var sum = 0

      channel.takeUntil(trigger).subscribe(
        elem => { sum += elem; Continue },
        ex => throw ex
      )

      for (_ <- 0 until 1000) {
        val ack = channel.onNext(1)
        expect(ack.value.get.get.toString).toBe("Continue")
      }

      trigger.pushComplete()
      jasmine.Clock.tick(1)
      expect(sum).toBe(1000)
    }

    it("should stop with error in case the other observable signals onError") {
      val trigger = PublishChannel[Unit]()
      val channel = PublishSubject[Int]()
      var errorThrown = null : Throwable
      var sum = 0

      channel.takeUntil(trigger).subscribe(
        elem => { sum += elem; Continue },
        ex => { errorThrown = ex }
      )

      for (_ <- 0 until 1000) {
        val ack = channel.onNext(1)
        expect(ack.value.get.get.toString).toBe("Continue")
      }

      trigger.pushError(new RuntimeException("DUMMY"))
      jasmine.Clock.tick(1)
      expect(errorThrown.getMessage).toBe("DUMMY")
      expect(sum).toBe(1000)
    }
  }
}
