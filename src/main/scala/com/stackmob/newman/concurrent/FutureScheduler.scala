package com.stackmob.newman.concurrent

import scala.concurrent._
import scala.concurrent.duration._
import java.util.concurrent.ConcurrentHashMap
import java.util.{Timer, UUID}
import scala.annotation.tailrec

/**
 * a utility to schedule the execution of a group of Futures, by key.
 * this class provides a facility on which to do mutual exclusion for futures, ensuring that only one future is currently executing on a given key
 * @tparam Key the type of the key on which futures will schedule themselves
 * @tparam Val the type of the values that each future will hold
 */
class FutureScheduler[Key, Val] {
  /**
   * each executing future for a given Key gets a UUID assigned to it.
   * this table keeps track of the currently executing one
   */
  private val executionTable = new ConcurrentHashMap[Key, UUID]()

  /**
   * runs {{{TimerTasks}}} that check the execution table to see if a future can execution
   */
  private val poller = new Timer("http-response-cacher-atomic-checker", true)

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
   * @param ctx the ExecutionContext to use to schedule {{{fut}}} for execution
   * @return a future that completes successfully when no other
   */
  def synchronize(key: Key)(fut: => Future[Val])(implicit ctx: ExecutionContext): Future[Val] = {
    val startPromise = Promise[Unit]()
    val curUUID = UUID.randomUUID()

    poller.schedule(50.milliseconds)(pollerCallback(key, curUUID, startPromise))
    val res = startPromise.future.flatMap { _ =>
      fut
    }
    res.onComplete { _ =>
      executionTable.remove(curUUID)
    }
    res
  }

}
