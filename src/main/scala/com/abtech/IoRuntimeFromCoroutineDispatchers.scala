package com.abtech

import cats.effect.unsafe.{IORuntime, IORuntimeBuilder, Scheduler}
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.{CoroutineDispatcher, Delay, Dispatchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

private val defaultDispatcher = Dispatchers.getDefault
private val blockingDispatcher = Dispatchers.getIO
private val delayInstance: Delay = kotlinx.coroutines.DelayKt.getDelay(EmptyCoroutineContext.INSTANCE)

private def executionContext(dispatcher: CoroutineDispatcher): ExecutionContext = {
  new ExecutionContext {
    final override def reportFailure(cause: Throwable): Unit =
      cause.printStackTrace()

    final override def execute(runnable: Runnable): Unit =
      dispatcher.dispatch(EmptyCoroutineContext.INSTANCE, runnable)
  }
}

private def scheduler() = new Scheduler {
  final override def sleep(delay: FiniteDuration, task: Runnable): Runnable = {
    val canceller = delayInstance.invokeOnTimeout(delay.toMillis, task, EmptyCoroutineContext.INSTANCE)
    () => canceller.dispose()
  }

  final override def nowMillis(): Long = System.currentTimeMillis()

  final override def monotonicNanos(): Long = System.nanoTime()
}

trait IoRuntimeFromCoroutineDispatcher {
  def ioRuntimeFromCoroutineDispatchers(): IORuntime = {
    IORuntimeBuilder()
      .setCompute(executionContext(defaultDispatcher), () => ())
      .setBlocking(executionContext(blockingDispatcher), () => ())
      .setScheduler(scheduler(), () => ())
      .build()
  }
}