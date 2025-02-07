package com.abtech

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.unsafe.IORuntime
import com.abtech.api.MyKotlinClass
import munit.CatsEffectSuite

import java.util.UUID
import scala.concurrent.duration.DurationDouble

final class KotlinInteropSpec extends CatsEffectSuite with KotlinInterop {
  interop =>
  test("make sure IO computes the right result") {
    val rt: IORuntime = interop.ioRuntimeFromCoroutineDispatchers()
    try {
      val tsk: IO[Unit] = testIoRt
      tsk.unsafeRunSync()(rt)
    } finally {
      rt.shutdown()
    }
  }

  test("make sure IO runs kotlin coroutine on computes thread-pool") {
    val myKotlinClass = new MyKotlinClass()
    val io: IO[String] = interop.ioCoroutine { cont =>
      myKotlinClass.hello("Abdhesh", cont)
    }
    io.map(assertEquals(_, "Hello, Abdhesh!"))
  }

  test("make sure IO runs kotlin coroutine on blocking thread-pool") {
    val myKotlinClass = new MyKotlinClass()
    val io: IO[String] = interop.ioCoroutineBlocking { cont =>
      myKotlinClass.hello("Abdhesh", cont)
    }
    io.map(assertEquals(_, "Hello, Abdhesh!"))
  }

  test("make sure IO computes kotlin coroutine context") {
    val myKotlinClass = new MyKotlinClass()
    val io: IO[String] = interop.ioCoroutine { cont =>
      myKotlinClass.helloWithContext("Abdhesh", cont)
    }
    io.map(assertEquals(_, "Hello, Abdhesh!"))
  }

  test("make sure IO computes kotlin coroutine context") {
    val io: IO[String] = IO.blocking{
      println(s"Hello, World! ${Thread.currentThread().getName}")
      "Hello, World!"
    }
    io.map(assertEquals(_, "Hello, World!"))
  }

  test("generate uuid") {
    object MockUUIDGen extends UUIDGen[IO] {
      override def randomUUID: IO[UUID] = IO.pure(UUID.fromString("12345678-1234-1234-1234-1234567890ab"))
    }
    implicit val mockUUIDGen: UUIDGen[IO] = MockUUIDGen
    val io: IO[String] = UUIDGen[IO].randomUUID.map(_.toString)
    io.map(println)
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