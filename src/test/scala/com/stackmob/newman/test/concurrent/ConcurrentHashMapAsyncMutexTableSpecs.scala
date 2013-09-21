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

package com.stackmob.newman.test
package concurrent

import org.specs2.Specification
import com.stackmob.newman.concurrent.{InMemoryAsyncMutex, ConcurrentHashMapAsyncMutexTable}
import scala.concurrent._
import java.util.concurrent.Executors

class ConcurrentHashMapAsyncMutexTableSpecs extends Specification { def is =
  "ConcurrentHashMapAsyncMutexTableSpecs".title                                                                         ^ end ^
  "ConcurrentHashMapAsyncMutexTable is an AsyncMutexTable backed by a concurrent hash map for storage"                  ^ end ^
  "mutex should return the same mutex for the same key"                                                                 ! sameMutex ^ end ^
  end

  private def sameMutex = {
    val mutex = new InMemoryAsyncMutex
    val createMutex = () => mutex
    val table = ConcurrentHashMapAsyncMutexTable[String](createMutex)
    implicit lazy val ctx = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
    val first = Future(table.mutex("one")).block() must beEqualTo(mutex)
    val second = Future(table.mutex("two")).block() must beEqualTo(mutex)
    first and second
  }
}
