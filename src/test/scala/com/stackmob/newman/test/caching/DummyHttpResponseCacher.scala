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
import scala.concurrent.Future

class DummyHttpResponseCacher(val onGet: Option[Future[HttpResponse]],
                              val onSet: Future[HttpResponse],
                              val onExists: Boolean,
                              val onRemove: Option[Future[HttpResponse]]) extends HttpResponseCacher {

  val getCalls = new CopyOnWriteArrayList[HttpRequest]()
  val setCalls = new CopyOnWriteArrayList[(HttpRequest, Future[HttpResponse])]()
  val removeCalls = new CopyOnWriteArrayList[HttpRequest]()
  val existsCalls = new CopyOnWriteArrayList[HttpRequest]()

  def totalNumCalls = getCalls.size() + setCalls.size() + existsCalls.size() + removeCalls.size()

  override def get(req: HttpRequest): Option[Future[HttpResponse]] = {
    getCalls.add(req)
    onGet
  }

  override def set(req: HttpRequest, resp: Future[HttpResponse]) = {
    setCalls.add(req -> resp)
    onSet
  }

  override def remove(req: HttpRequest): Option[Future[HttpResponse]] = {
    removeCalls.add(req)
    onRemove
  }

  override def exists(req: HttpRequest): Boolean = {
    existsCalls.add(req)
    onExists
  }
}
