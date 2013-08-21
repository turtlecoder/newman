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

package com.stackmob.newman.test.caching

import com.stackmob.newman.response.HttpResponse
import com.stackmob.newman.request.HttpRequest
import java.util.concurrent.CopyOnWriteArrayList
import com.stackmob.newman.caching._

class DummyHttpResponseCacher(onGet: => Option[HttpResponse],
                              onExists: => Boolean) extends HttpResponseCacher {

  def cannedGet = onGet
  def cannedExists = onExists

  val getCalls = new CopyOnWriteArrayList[HttpRequest]()
  val setCalls = new CopyOnWriteArrayList[(HttpRequest, HttpResponse)]()
  val existsCalls = new CopyOnWriteArrayList[HttpRequest]()
  def totalNumCalls = getCalls.size() + setCalls.size() + existsCalls.size()

  override def get(req: HttpRequest): Option[HttpResponse] = {
    getCalls.add(req)
    onGet
  }

  override def set(req: HttpRequest, resp: HttpResponse, ttl: Milliseconds) {
    setCalls.add(req -> resp)
  }

  override def exists(req: HttpRequest): Boolean = {
    existsCalls.add(req)
    onExists
  }
}
