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

package com.stackmob.newman

import com.stackmob.newman.request._
import com.stackmob.newman.caching._
import scalaz._
import Scalaz._
import scalaz.NonEmptyList._
import com.stackmob.newman.caching.HttpResponseCacher
import response.HttpResponse
import org.apache.http.HttpHeaders
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class ETagAwareHttpClient(httpClient: HttpClient,
                          httpResponseCacher: HttpResponseCacher,
                          t: Milliseconds)
                         (implicit ctx: ExecutionContext) extends HttpClient {
  import ETagAwareHttpClient._

  override def get(u: URL, h: Headers): GetRequest = new GetRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = {
      httpClient.get(u, h).apply
    }
    override val url = u
    override val headers = h
  }

  override def post(u: URL, h: Headers, b: RawBody): PostRequest = new PostRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = {
      httpClient.post(u, h, b).apply
    }
    override val url = u
    override val headers = h
    override val body = b
  }

  override def put(u: URL, h: Headers, b: RawBody): PutRequest = new PutRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = {
      httpClient.put(u, h, b).apply
    }
    override val url = u
    override val headers = h
    override val body = b
  }

  override def delete(u: URL, h: Headers): DeleteRequest = new DeleteRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers): Future[HttpResponse] = {
      httpClient.delete(u, h).apply
    }
    override val url = u
    override val headers = h
  }

  override def head(u: URL, h: Headers): HeadRequest = new HeadRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers): Future[HttpResponse] = {
      httpClient.head(u, h).apply
    }
    override val url = u
    override val headers = h
  }
}

object ETagAwareHttpClient {
  trait CachingMixin extends HttpRequest { this: HttpRequest =>
    protected implicit def ctx: ExecutionContext
    //the TTL for cached responses until they're purged and we go back to the server with no modified header
    protected def ttl: Milliseconds
    protected def cache: HttpResponseCacher
    protected def doHttpRequest(headers: Headers): Future[HttpResponse]

    private def addIfNoneMatch(h: Headers, eTag: String): Headers = h.map { headerList: HeaderList =>
      nel(HttpHeaders.IF_NONE_MATCH -> eTag, headerList.list.filterNot(_._1 === HttpHeaders.IF_NONE_MATCH))
    } orElse { Headers(HttpHeaders.IF_NONE_MATCH -> eTag) }

    private def cachedAndETagPresent(cached: HttpResponse,
                                     eTag: String,
                                     ttl: Milliseconds): Future[HttpResponse] = {
      val newHeaderList = addIfNoneMatch(this.headers, eTag)
      doHttpRequest(newHeaderList).map { response: HttpResponse =>
        if(response.notModified) {
          //not modified returned - so return cached response
          cached
        } else {
          //not modified was not returned, so cache new response and return it
          cache.set(this, response, ttl)
          response
        }
      }
    }

    private def cachedAndETagNotPresent: Future[HttpResponse] = notCached(ttl)

    private def notCached(ttl: Milliseconds): Future[HttpResponse] = {
      doHttpRequest(headers).map { response: HttpResponse =>
        //TODO: respect cache-control headers
        cache.set(this, response, ttl)
        response
      }
    }

    override def apply: Future[HttpResponse] = {
      cache.get(this).map { resp: HttpResponse =>
        resp.eTag some { eTag: String =>
          cachedAndETagPresent(resp, eTag, ttl)
        } none {
          cachedAndETagNotPresent
        }
      } | {
        notCached(ttl)
      }
    }
  }
}
