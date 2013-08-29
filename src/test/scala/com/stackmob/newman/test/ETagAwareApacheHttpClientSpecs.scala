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
import org.apache.http.HttpHeaders
import java.net.URL
import com.stackmob.newman._
import com.stackmob.newman.caching._
import com.stackmob.newman.response._
import com.stackmob.newman.request._
import com.stackmob.newman.test.caching._
import collection.JavaConverters._
import org.specs2.matcher.MatchResult
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class ETagAwareApacheHttpClientSpecs extends Specification { def is =
  "ETagAwareApacheHttpClientSpecs".title                                                                                ^ end ^
  """
  ETagAwareApacheHttpClient does the equivalent of ApacheHttpClient, except it interacts with an HttpResponseCacher
  in order to execute If-None-Match requests using an ETag (if one was present in a previously cached HTTPResponse)
  """                                                                                                                   ^ end ^
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

    protected val responseWithETag = Future.successful {
      HttpResponse(HttpResponseCode.Ok, Headers(eTag), body)
    }
    protected val responseWithoutETag = Future.successful {
      HttpResponse(HttpResponseCode.Ok, Headers.empty, body)
    }
    protected val responseWithNotModified = Future.successful {
      HttpResponse(HttpResponseCode.NotModified, Headers.empty, body)
    }

    protected lazy val client = new ETagAwareHttpClient(rawClient, responseCacher)

    protected def rawClient: DummyHttpClient
    protected def responseCacher: HttpResponseCacher
  }

  case class CachedResponseWithETag() extends Context {
    override protected val responseCacher = new DummyHttpResponseCacher(responseWithETag, Some(responseWithETag), Some(responseWithETag))

    override protected val rawClient = new DummyHttpClient

    def executesINMRequest = {
      client.get(url, Headers.empty).block()
      val urlCorrect = rawClient.headRequests.get(0)._1 must beEqualTo(url)
      val headersCorrect = rawClient.headRequests.get(0)._2 must haveTheSameHeadersAs(Headers(INM))
      urlCorrect and headersCorrect
    }
  }

  case class CachedResponseWithETagReturnsNotModified() extends Context {
    override protected val rawClient = new DummyHttpClient(responseWithNotModified)
    override protected val responseCacher = new DummyHttpResponseCacher(responseWithETag, Some(responseWithETag), Some(responseWithETag))

    def returnsCachedResponse = {
      val resp = client.get(url, Headers.empty).block()
      resp must beTheSameResponseAs(responseWithETag.block())
    }
  }

  case class CachedResponseWithETagReturnsModified() extends Context {
    override protected val rawClient = new DummyHttpClient(responseWithETag)
    override protected val responseCacher = new DummyHttpResponseCacher(responseWithETag, Some(responseWithETag), Some(responseWithETag))

    def returnsNewResponse = {
      val resp = client.get(url, Headers.empty).block()
      resp must beTheSameResponseAs(responseWithETag.block())
    }
  }

  case class CachedResponseWithoutETag() extends Context {
    override protected val rawClient = new DummyHttpClient(responseWithETag)
    override protected val responseCacher = new DummyHttpResponseCacher(onApply = responseWithoutETag,
      onGet = Some(responseWithoutETag),
      onRemove = Some(responseWithoutETag))

    def executesNormalRequest = {
      client.get(url, Headers.empty).block()
      (rawClient.getRequests.get(0)._1 must beEqualTo(url)) and
      (rawClient.getRequests.get(0)._2 must haveTheSameHeadersAs(Headers.empty))
    }

    def cachesNewResponse = {
      val req = client.get(url, Headers.empty)
      //wait for the request to finish. res isn't used
      val res = req.block()
      val getCallRes = responseCacher.verifyGetCalls { list =>
        val oneCall = list.length must beEqualTo(1)
        val firstCall = list(0) must beEqualTo(req)
        oneCall and firstCall
      }

      //TODO: verify removes and applies
      getCallRes
    }
  }

  case class NoCachedResponsePresent() extends Context {
    override protected val rawClient = new DummyHttpClient
    override protected val responseCacher = new DummyHttpResponseCacher(responseWithETag,
      Option.empty[Future[HttpResponse]],
      Option.empty[Future[HttpResponse]])

    def executesNormalRequest = {
      client.get(url, Headers.empty).block()
      val getRequest = rawClient.getRequests.get(0)
      (getRequest._1 must beEqualTo(url)) and
      (getRequest._2 must haveTheSameHeadersAs(DummyHttpClient.CannedResponse.headers))
    }

    def cachesNewResponse = {
      val req = client.get(url, Headers.empty)
      req.block()
      val getCallRes = responseCacher.verifyGetCalls { list =>
        val oneCall = list.length must beEqualTo(1)
        val firstCall = list(0) must beEqualTo(req)
        oneCall and firstCall
      }

      //TODO: verify removes and applies
      getCallRes
    }
  }

  case class CacheGetFailed() extends Context {
    private val cacheException = new Exception("couldn't hit cache")
    private val cacheExceptionFuture = Future.failed[HttpResponse](cacheException)
    override protected val rawClient = new DummyHttpClient
    override protected val responseCacher = new DummyHttpResponseCacher(cacheExceptionFuture, Some(cacheExceptionFuture), Some(cacheExceptionFuture))

    def executesNoRequest = {
      fromTryCatch(client.get(url, Headers.empty).block())
      (rawClient.totalNumRequestsMade must beEqualTo(0))
    }
  }
}
