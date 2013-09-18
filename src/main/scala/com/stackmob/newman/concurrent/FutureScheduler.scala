package com.stackmob.newman.concurrent

import scala.concurrent._
import scala.concurrent.duration._
import java.util.concurrent.ConcurrentHashMap
import java.util.{Timer, UUID}
import scala.annotation.tailrec
import FutureScheduler._

/**
 * a utility to schedule the execution of a group of Futures, by key.
 * this class provides a facility on which to do mutual exclusion for futures, ensuring that only one future is currently executing on a given key
 * @tparam Key the type of the key on which futures will schedule themselves
 * @tparam Val the type of the values that each future will hold
 */
class FutureScheduler[Key, Val](timer: Timer = DefaultTimer, pollingInterval: FiniteDuration =  10.milliseconds) {
  /**
   * each executing future for a given Key gets a UUID assigned to it.
   * this table keeps track of the currently executing one
   */
  private val executionTable = new ConcurrentHashMap[Key, UUID]()


  @tailrec
  private def pollerCallback(key: Key, uuid: UUID, promise: Promise[Unit]) {
    val cachedUUID = executionTable.putIfAbsent(key, uuid)
    if(cachedUUID == uuid) {
      promise.success(())
    } else {
      pollerCallback(key, uuid, promise)
    }
  }

  /**
   * get a Future that will successfully complete when no other future is currently executing on {{{key}}}
   * @param key the key to wait for
   * @param fut the future to execute atomically. no other future for the key will be executing during its lifecycle
   * @return a future that completes successfully when no other
   */
  def synchronize(key: Key)(fut: => Future[Val])(implicit ctx: ExecutionContext = SequentialExecutionContext): Future[Val] = {
    val startPromise = Promise[Unit]()
    val curUUID = UUID.randomUUID()

    timer.schedule(pollingInterval)(pollerCallback(key, curUUID, startPromise))
    val res = startPromise.future.flatMap { _ =>
      fut
    }(ctx)
    res.onComplete { _ =>
      executionTable.remove(curUUID)
    }(ctx)
    res
  }

}

object FutureScheduler {
  /**
   * runs {{{TimerTasks}}} that check the execution table to see if a future can execution
   */
  private[FutureScheduler] val DefaultTimer = new Timer("http-response-cacher-poller-timer", true)
}
