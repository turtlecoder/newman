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

/**
 * a HttpResponseCacher for testing
 * @param onApply the result to return from the apply method
 * @param foldBehavior whether to execute the cacheHit or cacheMiss parameters passed to the fold method.
 *                     pass Left(responseFuture) to execute cacheHit(responseFuture),
 *                     and pass Right(()) to execute cacheMiss
 */
class DummyHttpResponseCacher(val onApply: Future[HttpResponse],
                              val foldBehavior: Either[Future[HttpResponse], Unit]) extends HttpResponseCacher {

  val foldCalls = new CopyOnWriteArrayList[HttpRequest]()
  val applyCalls = new CopyOnWriteArrayList[HttpRequest]()

  def totalNumCalls = applyCalls.size() + foldCalls.size()

  override def fold(req: HttpRequest,
                    cacheHit: Future[HttpResponse] => Future[HttpResponse],
                    cacheMiss: => Future[HttpResponse]): Future[HttpResponse] = {
    foldCalls.add(req)
    foldBehavior match {
      case Left(respFuture) => cacheHit(respFuture)
      case Right(_) => cacheMiss
    }
  }

  override def apply(req: HttpRequest): Future[HttpResponse] = {
    applyCalls.add(req)
    onApply
  }

  def verifyApplyCalls(fn: List[HttpRequest] => MatchResult[_]): MatchResult[_] = {
    fn(applyCalls.asScala.toList)
  }

  def verifyFoldCalls(fn: List[HttpRequest] => MatchResult[_]): MatchResult[_] = {
    fn(foldCalls.asScala.toList)
  }
}
