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
   * get the given request from the cache, or execute it
   * @param req the request to get
   * @return the response, wrapped in a future. the future will be completed when the request finishes
   */
  def apply(req: HttpRequest): Future[HttpResponse]

  /**
   * get the given request form the cache
   * @param req the request to get
   * @return Some if the request exists in the cache, None otherwise
   */
  def get(req: HttpRequest): Option[Future[HttpResponse]]

  /**
   * remove the given request from the cache
   * @param req the request to remove
   * @return Some if the element existed, None otherwise
   */
  def remove(req: HttpRequest): Option[Future[HttpResponse]]
}
