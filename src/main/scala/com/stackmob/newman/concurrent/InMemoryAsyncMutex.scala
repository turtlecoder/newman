package com.stackmob.newman.concurrent

import scala.concurrent.Future
import com.twitter.concurrent.{AsyncMutex => TwAsyncMutex}

/**
 * an AsyncMutex backed by a {{{com.twitter.concurrent.AsyncMutex}}}
 */
class InMemoryAsyncMutex extends AsyncMutex {
  override def apply[T](fut: => Future[T]): Future[T] = {
    val twMutex = new TwAsyncMutex
    twMutex.acquire().toScalaFuture.flatMap { twPermit =>
      val f = fut
      f.onComplete { _ =>
        twPermit.release()
      }
      f
    }
  }
}
