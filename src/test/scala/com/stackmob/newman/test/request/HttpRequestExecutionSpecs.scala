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
import Scalaz._
import scalaz.effect.IO
import scalaz.NonEmptyList._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult, Failure => SpecsFailure}
import java.net.URL
import com.stackmob.newman.request._
import HttpRequestExecution._
import com.stackmob.newman.response._
import com.stackmob.newman._

class HttpRequestExecutionSpecs extends Specification { def is =
  "HttpRequestExecutionSpecs".title                                                                                     ^
  """
  HttpRequestExecution is an object that contains convenience methods for executing multiple HttpRequests in various
  ways like sequentially, chained (ie: generate the next request based on the previous response)
  """                                                                                                                   ^
  "HttpRequestExecution should"                                                                                         ^
    "execute http requests in sequence correctly"                                                                       ! ExecuteSequence().executesCorrectly ^
    "fail if one request fails"                                                                                         ! ExecuteSequence().allFailIfOneFails ^
    "execute chained http requests correctly"                                                                           ! ExecuteChained().executesCorrectly ^
    "fail if one chain method fails"                                                                                    ! ExecuteChained().allFailIfOneChainMethodFails ^
    "fail if one chained request fails"                                                                                 ! ExecuteChained().allFailIfOneRequestFails ^
    "execute concurrent requests correctly"                                                                             ! ExecuteConcurrent().executesCorrectly ^
    "fail if one concurrent request failed"                                                                             ! ExecuteConcurrent().allFailIfOneFails ^
                                                                                                                        end

  trait Context extends BaseContext {
    protected lazy val requestURL = new URL("http://stackmob.com")
    protected lazy val requestHeaders = Headers.empty

    protected lazy val client1 = new DummyHttpClient(response1.pure[Function0])
    protected lazy val request1 = client1.get(requestURL, requestHeaders)
    protected lazy val response1 = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)

    protected lazy val client2 = new DummyHttpClient(response2.pure[Function0])
    protected lazy val request2 = client2.get(requestURL, requestHeaders)
    protected lazy val response2 = HttpResponse(HttpResponseCode.Unauthorized, Headers.empty, RawBody.empty)

    protected lazy val exception = new Exception("test exception")
    protected lazy val throwingClient = new DummyHttpClient(() => throwingResponse)
    protected lazy val throwingRequest = throwingClient.get(requestURL, requestHeaders)
    protected lazy val throwingResponse: HttpResponse = throw exception

    protected def ensureThrows[T](io: IO[T], exception: Throwable): SpecsResult = validating(io.unsafePerformIO).map { _ =>
      SpecsFailure("didn't throw a %s with message %s when it should have".format(exception.getClass.getCanonicalName,
        exception.getMessage)): SpecsResult
    } match {
      case Success(s) => s
      case Failure(t) => t must beEqualTo(exception)
    }
  }

  case class ExecuteSequence() extends Context {
    def executesCorrectly: SpecsResult = {
      val requestList = nels(request1, request2)
      val expectedRequestResponseList = nels(request1 -> response1, request2 -> response2)
      val res = sequencedRequests(requestList)
      res.unsafePerformIO.list must beEqualTo(expectedRequestResponseList.list)
    }

    def allFailIfOneFails: SpecsResult = {
      val requestList = nels(request1, throwingRequest)
      val res = sequencedRequests(requestList)
      ensureThrows(res, exception)
    }
  }

  case class ExecuteChained() extends Context {
    private lazy val client3 = new DummyHttpClient(response3.pure[Function0])
    private lazy val request3 = client3.get(requestURL, requestHeaders)
    private lazy val response3 = HttpResponse(HttpResponseCode.Created, Headers.empty, RawBody.empty)
    private def chain1(prevResp: HttpResponse): HttpRequest = request2
    private def chain2(prevResp: HttpResponse): HttpRequest = request3
    private def chainThatReturnsThrowingRequest(prevResp: HttpResponse): HttpRequest = throwingRequest
    private def throwingChain(prevResp: HttpResponse): HttpRequest = throw exception
    def executesCorrectly: SpecsResult = {
      val requestList = request1 :: request2 :: request3 :: Nil
      val res = chainedRequests(request1, nels(chain1 _, chain2 _))
      res.unsafePerformIO.list must beEqualTo(requestList.zip(List(response1, response2, response3)))
    }

    def allFailIfOneRequestFails: SpecsResult = {
      val res = chainedRequests(request1, nels(chain1 _, chainThatReturnsThrowingRequest _))
      ensureThrows(res, exception)
    }

    def allFailIfOneChainMethodFails: SpecsResult = {
      val res = chainedRequests(request1, nels(chain1 _, throwingChain _))
      ensureThrows(res, exception)
    }
  }

  case class ExecuteConcurrent() extends Context {
    def executesCorrectly: SpecsResult = {
      val requestList = nels(request1, request2)
      val expectedRequestResponseList = nels(request1 -> response1, request2 -> response2)
      val res = concurrentRequests(requestList).map { list: RequestPromiseResponsePairList =>
        list.map(pair => pair._1 -> pair._2.get)
      }
      res.unsafePerformIO.list must beEqualTo(expectedRequestResponseList.list)
    }

    def allFailIfOneFails: SpecsResult = {
      val requestList = nels(request1, throwingRequest)
      val res = concurrentRequests(requestList).map { list: RequestPromiseResponsePairList =>
        list.map(pair => pair._1 -> pair._2.get)
      }
      ensureThrows(res, exception)
    }
  }
}
