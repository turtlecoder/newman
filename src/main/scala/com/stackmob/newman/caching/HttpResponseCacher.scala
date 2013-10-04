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

package com.stackmob.newman.caching

import com.stackmob.newman.response.HttpResponse
import com.stackmob.newman.request.HttpRequest
import scala.concurrent._

trait HttpResponseCacher {

  /**
   * get the given request from the cache. execute cacheHit if it was found, or cacheMiss if not
   * @param req the request to get
   * @param cacheHit the function to execute if there was a cache hit.
   *                 the cache will be replaced by the returned future.
   * @param cacheMiss the function to execute if there was a cache miss.
   *                  the cache will be replaced by the returned future.
   * @return the response, wrapped in a future.
   *         if there was a cache hit, the future will be completed when the future returned by cacheHit does.
   *         if there was a cache miss, the future will be completed when the future returned by cacheMiss does.
   */
  def fold(req: HttpRequest,
           cacheHit: Future[HttpResponse] => Future[HttpResponse],
           cacheMiss: => Future[HttpResponse]): Future[HttpResponse]

  /**
   * get the response future from the cache, or execute the request,
   * put its response future into the cache, and return it.
   * @param req the request whose corresponding response to look for in the cache
   * @return the response, wrapped in a future. the future will be completed when the request finishes,
   *         regardless of whether it was cached
   */
  def apply(req: HttpRequest): Future[HttpResponse]


}
