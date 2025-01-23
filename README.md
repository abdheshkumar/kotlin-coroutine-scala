# Use Kotlin Coroutine with Scala Cats-effect

```scala
import cats.effect.IO
import kotlin.coroutines.{Continuation, CoroutineContext, EmptyCoroutineContext}
import kotlinx.coroutines.{CoroutineScopeKt, CoroutineStart}
import kotlinx.coroutines.future.FutureKt

import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.jdk.FutureConverters._

object KotlinInterop extends KotlinInterop

private trait KotlinInterop extends IoRuntimeFromCoroutineDispatcher {

  private final def completableFutureFromCoroutine[A](callCrt: Continuation[? >: A] => AnyRef, context: CoroutineContext = EmptyCoroutineContext.INSTANCE): CompletableFuture[A] = {
    FutureKt.future(
      CoroutineScopeKt.CoroutineScope(EmptyCoroutineContext.INSTANCE),
      context,
      CoroutineStart.DEFAULT,
      (_, cont) => callCrt(cont),
    )
  }

  final def scalaFutureFromCoroutine[A](callCrt: Continuation[? >: A] => AnyRef): Future[A] = {
    this.completableFutureFromCoroutine[A](callCrt).asScala
  }

  final def ioCoroutine[A](callCrt: Continuation[? >: A] => AnyRef): IO[A] = {
    IO.executor
      .map(executor => kotlinx.coroutines.ExecutorsKt.from(executor))
      .flatMap { coroutineDispatcher =>
        IO.fromCompletableFuture(IO.delay(this.completableFutureFromCoroutine[A](callCrt, coroutineDispatcher)))
      }
  }

  final def ioCoroutineBlocking[A](callCrt: Continuation[? >: A] => AnyRef): IO[A] = {
    IO.executor
      .map(executor => kotlinx.coroutines.ExecutorsKt.from(executor))
      .flatMap { coroutineDispatcher =>
        IO.fromCompletableFuture(IO.blocking(this.completableFutureFromCoroutine[A](callCrt, coroutineDispatcher)))
      }
  }
}
```

How to use it:

```scala
package com.abtech

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.abtech.api.MyKotlinClass
import munit.CatsEffectSuite

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
```