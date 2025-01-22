package com.abtech

import cats.effect.IO
import com.abtech.api.MyKotlinClass
import munit.CatsEffectSuite
import scala.concurrent.duration.DurationDouble

final class KotlinInteropSpec extends CatsEffectSuite with KotlinInterop {
  interop =>
  test("make sure IO computes the right result") {
    val rt = interop.ioRuntimeFromCoroutineDispatchers()
    try {
      val tsk = testIoRt
      tsk.unsafeRunSync()(rt)
    } finally {
      rt.shutdown()
    }
  }

  test("make sure IO runs kotlin coroutine on computes thread-pool") {
    val myKotlinClass = new MyKotlinClass()
    val io = interop.ioCoroutine { cont =>
      myKotlinClass.hello("Abdhesh", cont)
    }
    io.map(assertEquals(_, "Hello, Abdhesh!"))
  }

  test("make sure IO runs kotlin coroutine on blocking thread-pool") {
    val myKotlinClass = new MyKotlinClass()
    val io = interop.ioCoroutineBlocking { cont =>
      myKotlinClass.hello("Abdhesh", cont)
    }
    io.map(assertEquals(_, "Hello, Abdhesh!"))
  }

  test("make sure IO computes kotlin coroutine context") {
    val myKotlinClass = new MyKotlinClass()
    val io = interop.ioCoroutine { cont =>
      myKotlinClass.helloWithContext("Abdhesh", cont)
    }
    io.map(assertEquals(_, "Hello, Abdhesh!"))
  }

  test("make sure IO computes kotlin coroutine context") {
    val io = IO.blocking{
      println(s"Hello, World! ${Thread.currentThread().getName}")
      "Hello, World!"
    }
    io.map(assertEquals(_, "Hello, World!"))
  }

  private def testIoRt: IO[Unit] = {
    for {
      _ <- IO.cede // go to compute pool
      v <- IO.blocking {
        42
      } // go to blocking pool
      _ <- IO(assertEquals(v, 42))
      _ <- IO.cede // go back to compute pool
      _ <- IO.sleep(0.1.second) // test scheduler
      fib <- IO.pure(42).delayBy(0.1.second).start
      v <- fib.joinWithNever
      _ <- IO(assertEquals(v, 42))
    } yield ()
  }
}