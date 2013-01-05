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

package com.stackmob.newman.caching

import java.util.concurrent.ConcurrentHashMap
import com.stackmob.newman.response.HttpResponse
import com.stackmob.newman.request.HttpRequest
import scalaz.effects._

class InMemoryHttpResponseCacher extends HttpResponseCacher {
  private val cache = new ConcurrentHashMap[Array[Byte], HttpResponse]()

  override def get(req: HttpRequest): IO[Option[HttpResponse]] = io(Option(cache.get(req.hash)))
  override def set(req: HttpRequest, resp: HttpResponse): IO[Unit] = io {
    cache.put(req.hash, resp)
    ()
  }

  override def exists(req: HttpRequest): IO[Boolean] = io(cache.containsKey(req.hash))
}
