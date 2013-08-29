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
import org.specs2.matcher.MatchResult
import scala.collection.JavaConverters._

class DummyHttpResponseCacher(val onApply: Future[HttpResponse],
                              val onGet: Option[Future[HttpResponse]],
                              val onRemove: Option[Future[HttpResponse]]) extends HttpResponseCacher {

  val applyCalls = new CopyOnWriteArrayList[HttpRequest]()
  val getCalls = new CopyOnWriteArrayList[HttpRequest]()
  val removeCalls = new CopyOnWriteArrayList[HttpRequest]()

  def totalNumCalls = applyCalls.size() + getCalls.size() + removeCalls.size()

  override def apply(req: HttpRequest): Future[HttpResponse] = {
    applyCalls.add(req)
    onApply
  }

  override def get(req: HttpRequest): Option[Future[HttpResponse]] = {
    getCalls.add(req)
    onGet
  }

  override def remove(req: HttpRequest): Option[Future[HttpResponse]] = {
    removeCalls.add(req)
    onRemove
  }

  def verifyApplyCalls(fn: List[HttpRequest] => MatchResult[_]): MatchResult[_] = {
    fn(applyCalls.asScala.toList)
  }

  def verifyGetCalls(fn: List[HttpRequest] => MatchResult[_]): MatchResult[_] = {
    fn(getCalls.asScala.toList)
  }

  def verifyRemoveCalls(fn: List[HttpRequest] => MatchResult[_]): MatchResult[_] = {
    fn(removeCalls.asScala.toList)
  }
}
