/**
 * Copyright 2012 StackMob
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

package com.stackmob.newman
package caching

import java.util.concurrent._
import com.stackmob.newman.response.HttpResponse
import com.stackmob.newman.request.HttpRequest
import scalaz.effects._

case class CachedResponseDelay(ttl: Milliseconds, hash: HashCode) extends Delayed {
  private val start = Milliseconds.current
  private def endMilliseconds = start ++ ttl

  override def getDelay(unit: TimeUnit): Long = {
    unit.convert(endMilliseconds.magnitude - Milliseconds.current.magnitude, TimeUnit.MILLISECONDS)
  }

  override def compareTo(d: Delayed): Int = {
    val otherDelay = Milliseconds(d.getDelay(TimeUnit.MILLISECONDS))
    endMilliseconds.compareTo(otherDelay)
  }
}

class InMemoryHttpResponseCacher extends HttpResponseCacher {

  Executors.newSingleThreadExecutor().submit(delayQueueRunnable)

  private lazy val cache = new ConcurrentHashMap[HashCode, HttpResponse]()
  private lazy val delayQueue = new DelayQueue[CachedResponseDelay]()

  private lazy val delayQueueRunnable = new Runnable {
    def run() {
      while(true) {
        validating {
          val hash = delayQueue.take().hash
          cache.remove(hash)
        }
      }
    }
  }

  override def get(req: HttpRequest): IO[Option[HttpResponse]] = io {
    Option(cache.get(req.hash))
  }

  override def set(req: HttpRequest, resp: HttpResponse, ttl: Milliseconds): IO[Unit] = io {
    cache.put(req.hash, resp)
    delayQueue.add(CachedResponseDelay(ttl, req.hash))
    ()
  }

  override def exists(req: HttpRequest): IO[Boolean] = io(cache.containsKey(req.hash))
}
