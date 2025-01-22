package com.abtech

import cats.effect.IO
import kotlin.coroutines.{Continuation, CoroutineContext}
import kotlinx.coroutines.{CoroutineContextKt, CoroutineDispatcher, CoroutineScope, CoroutineStart, Deferred, DisposableHandle, GlobalScope}

import java.util
import java.util.concurrent.CancellationException
import scala.util.control.{NoStackTrace, NonFatal}

object KotlinCoroutines {
  def runCancelable_(
                      block: (CoroutineScope, Continuation[? >: kotlin.Unit]) => Any
                    ): IO[Unit] = {
    runCancelable(block).void
  }

  def runCancelable[A](
                        block: (CoroutineScope, Continuation[? >: A]) => Any
                      ): IO[A] = {
    coroutineToIOFactory[A](block, buildCancelToken)
  }

  private def dispatcher: IO[CoroutineDispatcher] =
    IO.executor.map { executor =>
      kotlinx.coroutines.ExecutorsKt.from(executor)
    }

  private def coroutineToIOFactory[A](
                                       block: (CoroutineScope, Continuation[? >: A]) => Any,
                                       buildCancelToken: (Deferred[?], DisposableHandle) => Option[IO[Unit]]
                                     ): IO[A] = {
    dispatcher.flatMap { dispatcher =>
      IO.async[A] { cb =>
        IO {
          try {
            val context = CoroutineContextKt.newCoroutineContext(
              GlobalScope.INSTANCE,
              dispatcher,
            )
            val deferred = kotlinx.coroutines.BuildersKt.async(
              GlobalScope.INSTANCE,
              context,
              CoroutineStart.DEFAULT,
              (currentScope: CoroutineScope, currentContinuation: Continuation[? >: A]) => block(currentScope, currentContinuation)
            )
            try {
              val dispose = deferred.invokeOnCompletion(
                (e: Throwable) => {
                  e match {
                    case null => cb(Right(deferred.getCompleted))
                    case e: Throwable => cb(Left(e))
                  }
                  kotlin.Unit.INSTANCE
                })
              buildCancelToken(deferred, dispose)
            } catch {
              case NonFatal(e) =>
                deferred.cancel(null)
                throw e
            }
          } catch {
            case NonFatal(e) =>
              cb(Left(e))
              None
          }
        }
      }
    }.recoverWith {
      case PleaseCancel =>
        // This branch actually never happens, but it might
        // prevent leaks in case of a bug
        IO.canceled *> IO.never
    }
  }

  private def buildCancelToken(deferred: Deferred[?], dispose: DisposableHandle): Option[IO[Unit]] =
    Some(IO.defer {
      deferred.cancel(PleaseCancel)
      dispose.dispose()
      // Await for completion or cancellation
      coroutineToIOFactory[kotlin.Unit](
        (_, cont) => deferred.join(cont),
        (_, _) => None
      ).void
    })

  private object PleaseCancel
    extends CancellationException with NoStackTrace
}