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

package com.stackmob.newman.test
package request

import scalaz._
import scalaz.Validation._
import Scalaz._
import scalaz.NonEmptyList._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult, Failure => SpecsFailure}
import java.net.URL
import com.stackmob.newman.request._
import HttpRequestExecution._
import com.stackmob.newman.response._
import com.stackmob.newman._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.Future

class HttpRequestExecutionSpecs extends Specification { def is =
  "HttpRequestExecutionSpecs".title                                                                                     ^
  """
  HttpRequestExecution is an object that contains convenience methods for executing multiple HttpRequests in various
  ways like sequentially, chained (ie: generate the next request based on the previous response)
  """                                                                                                                   ^
  "HttpRequestExecution should"                                                                                         ^
    "execute http requests in sequence correctly"                                                                       ! ExecuteSequence().executesCorrectly ^
    "execute concurrent requests correctly"                                                                             ! ExecuteConcurrent().executesCorrectly ^
                                                                                                                        end
  trait Context extends BaseContext {
    protected lazy val requestURL = new URL("http://stackmob.com")
    protected lazy val requestHeaders = Headers.empty

    protected lazy val client1 = new DummyHttpClient(response1)
    protected lazy val request1 = client1.get(requestURL, requestHeaders)
    protected lazy val response1 = Future.successful {
      HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    }

    protected lazy val client2 = new DummyHttpClient(response2)
    protected lazy val request2 = client2.get(requestURL, requestHeaders)
    protected lazy val response2 = Future.successful {
      HttpResponse(HttpResponseCode.Unauthorized, Headers.empty, RawBody.empty)
    }

    protected lazy val exception = new Exception("test exception")
    protected lazy val throwingClient = new DummyHttpClient(throwingResponse)
    protected lazy val throwingRequest = throwingClient.get(requestURL, requestHeaders)
    protected lazy val throwingResponse = Future.failed[HttpResponse] {
      exception
    }
  }

  case class ExecuteSequence() extends Context {
    def executesCorrectly: SpecsResult = {
      val requestList = List(request1, request2)
      val expectedRequestResponseList = List(request1 -> response1, request2 -> response2)
      val res = sequencedRequests(requestList)
      res must beEqualTo(expectedRequestResponseList)
    }

    def timeoutIfFirstFutureFails() = {
      val requestList = List(throwingRequest, request2)
      val res = sequencedRequests(requestList)
      val first = res.apply(0)._2.toEither() must beLeft
      val second = res.apply(1)._2.toEither() must beRight
      first and second
    }
  }

  case class ExecuteConcurrent() extends Context {
    def executesCorrectly = {
      val requestList = List(request1, request2)
      val expectedRequestResponseList = List(request1 -> response1, request2 -> response2)
      val res = concurrentRequests(requestList).map { tup =>
        val (req, respFut) = tup
        req -> respFut.block()
      }
      res must beEqualTo(expectedRequestResponseList)
    }

    def allFailIfOneFails: SpecsResult = {
      val requestList = List(request1, throwingRequest)
      val res = concurrentRequests(requestList)

      val oneThrows = res must haveOneElementLike {
        case tup => tup._2.toEither() must beLeft
      }
      val oneSucceeds = res must haveOneElementLike {
        case tup => tup._2.toEither() must beRight
      }
      oneThrows and oneSucceeds
    }
  }
}
