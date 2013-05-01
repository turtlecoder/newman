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
import scalaz.effect.IO

trait HttpResponseCacher {
  /**
   * possibly get a response for the given request
   * @param req the request
   * @return an IO representing the response, or none if none exists
   */
  def get(req: HttpRequest): IO[Option[HttpResponse]]

  /**
   * set a response for the given request, for a TTL
   * @param req the request
   * @param resp the response for the given request
   * @param ttl the time to live for the given request/response pair
   * @return the IO representing the set action
   */
  def set(req: HttpRequest, resp: HttpResponse, ttl: Milliseconds): IO[Unit]

  /**
   * determine whether a response for the given request exists
   * @param req the request
   * @return the action to determine existence. will contain true if it does, false otherwise.
   *         note that if the resultant IO is true, a subsequent get call may still not return a response
   */
  def exists(req: HttpRequest): IO[Boolean]
}
