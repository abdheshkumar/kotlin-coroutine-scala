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