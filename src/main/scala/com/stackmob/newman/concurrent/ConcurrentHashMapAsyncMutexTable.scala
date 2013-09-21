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

import java.util.concurrent.ConcurrentHashMap

/**
 * an AsyncMutexTable backed by a ConcurrentHashMap
 * @tparam Key the keys which identify each AsyncMutex in the table
 * @param f a function to create new {{{AsyncMutex}}}es to be used to fill the table if there's a miss
 */
class ConcurrentHashMapAsyncMutexTable[Key](f: () => AsyncMutex) extends AsyncMutexTable[Key] {
  private lazy val table = new ConcurrentHashMap[Key, AsyncMutex]()

  override def filler: AsyncMutex = f()

  override def mutex(key: Key): AsyncMutex = {
    val newMutex = filler
    Option(table.putIfAbsent(key, newMutex)).getOrElse(newMutex)
  }
}

object ConcurrentHashMapAsyncMutexTable {
  def apply[Key](filler: () => AsyncMutex): ConcurrentHashMapAsyncMutexTable[Key] = {
    new ConcurrentHashMapAsyncMutexTable[Key](filler)
  }
}