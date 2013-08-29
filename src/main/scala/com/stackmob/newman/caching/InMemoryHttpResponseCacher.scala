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

package com.stackmob.newman
package caching

import com.stackmob.newman.response.HttpResponse
import com.stackmob.newman.request.HttpRequest
import spray.caching._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 * an {{{HttpResponseCacher}}} that does caching in-memory, and evicts based on Least-Recently-Used
 * @param maxCapacity the maximum capacity of the cache
 * @param initialCapacity the starting capacity of the cache. must be < {{{maxCapacity}}}
 * @param timeToLive the maximum time an element is allowed to live in the cache
 * @param timeToIdle the maximum time an element is allowed to live in the cache untouched
 * @param ctx the execution context used to set elements into the cache
 */
class InMemoryHttpResponseCacher(maxCapacity: Int,
                                 initialCapacity: Int,
                                 timeToLive: Duration,
                                 timeToIdle: Duration)
                                (implicit ctx: ExecutionContext) extends HttpResponseCacher {

  private val cache = LruCache.apply[HttpResponse](maxCapacity = maxCapacity,
    initialCapacity = initialCapacity,
    timeToLive = timeToLive,
    timeToIdle = timeToIdle)

  override def get(req: HttpRequest): Option[Future[HttpResponse]] = {
    cache.get(req)
  }

  override def set(req: HttpRequest, resp: Future[HttpResponse]) = {
    cache.apply(req)(resp)
  }

  override def remove(req: HttpRequest): Option[Future[HttpResponse]] = {
    cache.remove(req)
  }
}
