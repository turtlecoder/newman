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

/**
 * a table of {{{com.stackmob.newman.concurrent.AsyncMutex}}}es, each of which is indexed by a key.
 * example usage:
 * {{{
 *   val table: AsyncMutexTable = ...
 *   lazy val f1 = Future(longOperation)
 *   lazy val f2 = Future(longOperation)
 *   lazy val f3 = Future(longOperation)
 *   lazy val f4 = Future(longOperation)
 *   //{f1, f2} and {f3, f4} may execute concurrently, but {f1, f2} are mutually exclusive and same for {f3, f4}
 *   table("operation1")(f1)
 *   table("operation1"(f2)
 *   table("operation2")(f3)
 *   table("operation2")(f4)
 * }}}
 * @tparam Key the keys which identify each AsyncMutex in the table
 */
trait AsyncMutexTable[Key] {
  /**
   * atomically get or create an {{{AsyncMutex}}} for the given key and then call withLock on that AsyncMutex with the given future.
   * note that this method is not atomic between the get-or-create and the withLock call
   * @param key the key to identify the AsyncMutex
   * @param fut the future to execute once the AsyncMutex was retrieved
   * @tparam T the return type of the future
   * @return a future that completes after the following steps have occurred:
   *         - the lock was acquired
   *         - the given future has completed
   *         - the lock was released (but not necessarily acquired by someone else, so this operation is fast)
   */
  def apply[T](key: Key)(fut: => Future[T]): Future[T] = {
    mutex(key).apply(fut)
  }

  /**
   * create a new AsyncMutex to put in the table if there wasn't one already associated with a key
   * @return
   */
  protected def filler: AsyncMutex

  /**
   * get the mutex for a given key
   * @param key the key whose mutex to get
   * @return the mutex for the given key
   */
  def mutex(key: Key): AsyncMutex
}
