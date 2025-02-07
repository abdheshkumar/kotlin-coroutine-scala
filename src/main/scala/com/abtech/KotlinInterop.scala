package com.abtech

import cats.effect.IO
import kotlin.coroutines.{Continuation, CoroutineContext, EmptyCoroutineContext}
import kotlinx.coroutines.future.FutureKt
import kotlinx.coroutines.{AbstractCoroutine, CoroutineContextKt, CoroutineScopeKt, CoroutineStart}

import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.jdk.FutureConverters.*

object KotlinInterop extends KotlinInterop

private trait KotlinInterop extends IoRuntimeFromCoroutineDispatcher {

  final def ioFromCoroutine[A](callCrt: Continuation[? >: A] => AnyRef, context: CoroutineContext = EmptyCoroutineContext.INSTANCE): IO[A] = {
    IO.async_ { cb =>
      val newContext = CoroutineContextKt.newCoroutineContext(EmptyCoroutineContext.INSTANCE, context)
      val coroutine = new AbstractCoroutine[A](context, true, true) {
        override def onCompleted(value: A): Unit = cb(Right(value))

        override def onCancelled(cause: Throwable, handled: Boolean): Unit = cb(Left(cause))
      }
      coroutine.start(CoroutineStart.DEFAULT, coroutine, (_, cont) => callCrt(cont))
    }
  }

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