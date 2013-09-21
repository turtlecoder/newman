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

package com.stackmob.newman.test.concurrent

import org.specs2.Specification
import com.stackmob.newman.concurrent.InMemoryAsyncMutex
import scala.concurrent._
import java.util.concurrent.{Executors, CountDownLatch}
import java.util.concurrent.atomic.AtomicBoolean

class InMemoryAsyncMutexSpecs extends Specification { def is =
  "InMemoryAsyncMutexSpecs".title                                                                                       ^ end ^
  "InMemoryAsyncMutex is the AsyncMutex implementation that's based on Twitter's AsyncMutex class"                      ^ end ^
  "a future should wait until a previous future holding the lock finishes"                                              ! waitsForLock ^ end ^
  end

  private def waitsForLock = {
    val mutex = new InMemoryAsyncMutex
    val f1StartLatch = new CountDownLatch(1)
    val f1Started = new AtomicBoolean(false)
    val f1EndLatch = new CountDownLatch(1)

    val f2StartLatch = new CountDownLatch(1)
    val f2Started = new AtomicBoolean(false)
    val f2EndLatch = new CountDownLatch(1)

    implicit lazy val ctx = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool)

    lazy val f1 = Future {
      f1StartLatch.await()
      f1Started.set(true)
      f1EndLatch.await()
    }
    lazy val f2 = Future {
      f2StartLatch.await()
      f2Started.set(true)
      f2EndLatch.await()
    }

    mutex(f1)
    mutex(f2)

    val noneStarted = (f1Started.get must beEqualTo(false)) and (f2Started.get must beEqualTo(false))
    f1StartLatch.countDown()
    f2StartLatch.countDown()
    val oneStarted = (f1Started.get must beEqualTo(true)) or (f2Started.get must beEqualTo(true))
    f1EndLatch.countDown()
    f2EndLatch.countDown()
    val bothStarted = (f1Started.get must beEqualTo(true)) and (f2Started.get must beEqualTo(true))

    noneStarted and oneStarted and bothStarted
  }

}
