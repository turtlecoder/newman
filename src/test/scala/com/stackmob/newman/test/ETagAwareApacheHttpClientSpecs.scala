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

import scalaz._
import scalaz.Validation._
import Scalaz._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import org.apache.http.HttpHeaders
import java.net.URL
import com.stackmob.newman._
import com.stackmob.newman.caching._
import com.stackmob.newman.response._
import com.stackmob.newman.request._
import com.stackmob.newman.test.caching._
import collection.JavaConverters._
import org.specs2.matcher.MatchResult
import scala.concurrent.ExecutionContext.Implicits.global

class ETagAwareApacheHttpClientSpecs extends Specification { def is =
  "ETagAwareApacheHttpClientSpecs".title                                                                                ^
  """
  ETagAwareApacheHttpClient does the equivalent of ApacheHttpClient, except it interacts with an HttpResponseCacher
  in order to execute If-None-Match requests using an ETag (if one was present in a previously cached HTTPResponse)
  """                                                                                                                   ^
  "CachingMixin should"                                                                                                 ^
    "execute an If-None-Match request if a cached response was present with an ETag"                                    ! CachedResponseWithETag().executesINMRequest ^
    "return the cached response if an INM request was executed and Not Modified was returned"                           ! CachedResponseWithETagReturnsNotModified().returnsCachedResponse ^
    "return the new response if an INM request was executed and something other than Not Modified was returned"         ! CachedResponseWithETagReturnsModified().returnsNewResponse ^
    "execute a request without If-None-Match if a cached response was present without an ETag"                          ! CachedResponseWithoutETag().executesNormalRequest ^
    "execute a request without If-None-Match if a cached response was not present"                                      ! NoCachedResponsePresent().executesNormalRequest ^
    "execute a request without If-None-Match if checking the cached failed"                                             ! CacheGetFailed().executesNoRequest ^
    "cache the new response when a cached response was present without an ETag"                                         ! CachedResponseWithoutETag().cachesNewResponse ^
    "cache the new response when the old response was not cached"                                                       ! NoCachedResponsePresent().cachesNewResponse ^
                                                                                                                        end

  trait Context extends BaseContext {
    protected val url = new URL("http://stackmob.com")

    protected val eTagVal = "testETag"
    protected val eTag: Header = HttpHeaders.ETAG -> eTagVal
    protected val INM: Header = HttpHeaders.IF_NONE_MATCH -> eTagVal
    protected val body = "testBody".getBytes

    protected val responseWithETag = HttpResponse(HttpResponseCode.Ok, Headers(eTag), body)
    protected val responseWithoutETag = HttpResponse(HttpResponseCode.Ok, Headers.empty, body)
    protected val responseWithNotModified = HttpResponse(HttpResponseCode.NotModified, Headers.empty, body)

    protected lazy val client = new ETagAwareHttpClient(rawClient, responseCacher, Milliseconds.current)

    protected def rawClient: DummyHttpClient
    protected def responseCacher: HttpResponseCacher

    def foldResponseCacherCalls(c: DummyHttpResponseCacher,
                                getFn: List[HttpRequest] => MatchResult[_],
                                setFn: List[(HttpRequest, HttpResponse)] => MatchResult[_]): MatchResult[_] = {
      (c.existsCalls.size must beEqualTo(0)) and
      (getFn(c.getCalls.asScala.toList)) and
      (setFn(c.setCalls.asScala.toList))
    }
  }

  case class CachedResponseWithETag() extends Context {
    override protected val responseCacher = new DummyHttpResponseCacher(responseWithETag.some, (), true)

    override protected val rawClient = new DummyHttpClient

    def executesINMRequest: SpecsResult = {
      client.get(url, Headers.empty).prepare.unsafePerformIO()
      (rawClient.getRequests.get(0)._1 must beEqualTo(url)) and
      (rawClient.getRequests.get(0)._2 must haveTheSameHeadersAs(Headers(INM)))
    }
  }

  case class CachedResponseWithETagReturnsNotModified() extends Context {
    override protected val rawClient = new DummyHttpClient(responseWithNotModified.pure[Function0])
    override protected val responseCacher = new DummyHttpResponseCacher(responseWithETag.some, (), true)

    def returnsCachedResponse: SpecsResult = {
      val resp = client.get(url, Headers.empty).prepare.unsafePerformIO()
      resp must beTheSameResponseAs(responseWithETag)
    }
  }

  case class CachedResponseWithETagReturnsModified() extends Context {
    override protected val rawClient = new DummyHttpClient(responseWithETag.pure[Function0])
    override protected val responseCacher = new DummyHttpResponseCacher(responseWithETag.some, (), true)

    def returnsNewResponse: SpecsResult = {
      val resp = client.get(url, Headers.empty).prepare.unsafePerformIO()
      resp must beTheSameResponseAs(responseWithETag)
    }
  }

  case class CachedResponseWithoutETag() extends Context {
    override protected val rawClient = new DummyHttpClient(responseWithETag.pure[Function0])
    override protected val responseCacher = new DummyHttpResponseCacher(responseWithoutETag.some, (), true)

    def executesNormalRequest: SpecsResult = {
      client.get(url, Headers.empty).prepare.unsafePerformIO()
      (rawClient.getRequests.get(0)._1 must beEqualTo(url)) and
      (rawClient.getRequests.get(0)._2 must haveTheSameHeadersAs(Headers.empty))
    }

    def cachesNewResponse: SpecsResult = {
      val req = client.get(url, Headers.empty)
      req.prepare.unsafePerformIO()
      foldResponseCacherCalls(responseCacher, { getCalls: List[HttpRequest] =>
        (getCalls.length must beEqualTo(1)) and
        (getCalls(0) must beEqualTo(req))
      }, { setCalls: List[(HttpRequest, HttpResponse)] =>
        (setCalls.length must beEqualTo(1)) and
        (setCalls(0) must beEqualTo(req -> responseWithETag))
      })
    }
  }

  case class NoCachedResponsePresent() extends Context {
    override protected val rawClient = new DummyHttpClient
    override protected val responseCacher = new DummyHttpResponseCacher(Option.empty[HttpResponse], (), true)

    def executesNormalRequest: SpecsResult = {
      client.get(url, Headers.empty).prepare.unsafePerformIO()
      val getRequest = rawClient.getRequests.get(0)
      (getRequest._1 must beEqualTo(url)) and
      (getRequest._2 must haveTheSameHeadersAs(DummyHttpClient.CannedResponse.headers))
    }

    def cachesNewResponse: SpecsResult = {
      val req = client.get(url, Headers.empty)
      req.prepare.unsafePerformIO()
      foldResponseCacherCalls(responseCacher, { getCalls: List[HttpRequest] =>
        (getCalls.length must beEqualTo(1)) and
        (getCalls(0) must beEqualTo(req))
      }, { setCalls: List[(HttpRequest, HttpResponse)] =>
        (setCalls.length must beEqualTo(1)) and
        (setCalls(0) must beEqualTo(req -> DummyHttpClient.CannedResponse))
      })
    }
  }

  case class CacheGetFailed() extends Context {
    private val cacheException = new Exception("couldn't hit cache")
    override protected val rawClient = new DummyHttpClient
    override protected val responseCacher = new DummyHttpResponseCacher((throw cacheException): Option[HttpResponse],
      (throw cacheException): Unit,
      (throw cacheException): Boolean)

    def executesNoRequest: SpecsResult = {
      fromTryCatch(client.get(url, Headers.empty).prepare.unsafePerformIO())
      (rawClient.totalNumRequestsMade must beEqualTo(0))
    }
  }
}
