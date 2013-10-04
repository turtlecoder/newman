/**
 * Copyright 2012-2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.newman.concurrent

import scala.concurrent.Future
/*
 * a non-blocking, asynchronous mutex. the concept was taken directly from Twitter's com.twitter.concurrent.AsyncMutex
 * https://github.com/twitter/util/blob/master/util-core/src/main/scala/com/twitter/concurrent/AsyncMutex.scala
 *
 * example usage:
 * {{{
 * val mutex: AsyncMutex = ...
 * lazy val f1 = mutex(Future(longOperation1))
 * lazy val f2 = mutex(Future(longOperation2))
 * }}}
 *
 * AsyncMutex is normally used via an {{{AsyncMutexTable}}} instead of used directly
 */
trait AsyncMutex {
  /**
   * acquires a lock before starting execution of a future, and releases the lock after the future completes.
   * @param fut the future to execute inside the critical section
   * @tparam T the return type of the future
   * @return a future that completes after the following steps have occurred:
   *         - the lock was acquired
   *         - the given future has completed
   *         - the lock was released (but not necessarily acquired by someone else, so this operation is fast)
   */
  def apply[T](fut: => Future[T]): Future[T]
}
