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
