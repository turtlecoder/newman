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
import scalaz.effect.IO
import scalaz.NonEmptyList._
import com.stackmob.newman.caching.HttpResponseCacher
import response.HttpResponse
import org.apache.http.HttpHeaders
import java.net.URL
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ETagAwareHttpClient(httpClient: HttpClient,
                          httpResponseCacher: HttpResponseCacher,
                          t: Milliseconds,
                          defaultDuration: Duration = 500.milliseconds)
                         (implicit ctx: ExecutionContext) extends HttpClient {
  import ETagAwareHttpClient._

  override def get(u: URL, h: Headers): GetRequest = new GetRequest with CachingMixin {
    override protected lazy val duration = defaultDuration
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers, d: Duration) = {
      httpClient.get(u, h).prepare(d)
    }
    override val url = u
    override val headers = h
  }

  override def post(u: URL, h: Headers, b: RawBody): PostRequest = new PostRequest with CachingMixin {
    override protected lazy val duration = defaultDuration
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers, d: Duration) = {
      httpClient.post(u, h, b).prepare(d)
    }
    override val url = u
    override val headers = h
    override val body = b
  }

  override def put(u: URL, h: Headers, b: RawBody): PutRequest = new PutRequest with CachingMixin {
    override protected lazy val duration = defaultDuration
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers, d: Duration) = {
      httpClient.put(u, h, b).prepare(d)
    }
    override val url = u
    override val headers = h
    override val body = b
  }

  override def delete(u: URL, h: Headers): DeleteRequest = new DeleteRequest with CachingMixin {
    override protected lazy val duration = defaultDuration
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers, d: Duration) = {
      httpClient.delete(u, h).prepare(d)
    }
    override val url = u
    override val headers = h
  }

  override def head(u: URL, h: Headers): HeadRequest = new HeadRequest with CachingMixin {
    override protected lazy val duration = defaultDuration
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers, d: Duration) = {
      httpClient.head(u, h).prepare(d)
    }
    override val url = u
    override val headers = h
  }
}

object ETagAwareHttpClient {
  trait CachingMixin extends HttpRequest { this: HttpRequest =>
    //in this implementation, requests have to block to fill the cache. this is how lone we'll wait before failing
    protected def duration: Duration
    //the TTL for cached responses until they're purged and we go back to the server with no modified header
    protected def ttl: Milliseconds
    protected def cache: HttpResponseCacher
    protected def doHttpRequest(headers: Headers, d: Duration): IO[HttpResponse]
    private lazy val cacheResult = cache.get(this)

    private def addIfNoneMatch(h: Headers, eTag: String): Headers = h.map { headerList: HeaderList =>
      nel(HttpHeaders.IF_NONE_MATCH -> eTag, headerList.list.filterNot(_._1 === HttpHeaders.IF_NONE_MATCH))
    } orElse { Headers(HttpHeaders.IF_NONE_MATCH -> eTag) }

    private def cachedAndETagPresent(cached: HttpResponse,
                                     eTag: String,
                                     ttl: Milliseconds,
                                     d: Duration): IO[Future[HttpResponse]] = {
      val newHeaderList = addIfNoneMatch(this.headers, eTag)
      doHttpRequest(newHeaderList, d).flatMap { response: HttpResponse =>
        if(response.notModified) {
          //not modified returned - so return cached response
          Future.successful(cached).pure[IO]
        } else {
          //not modified was not returned, so cache new response and return it
          cache.set(this, response, ttl).map { _ =>
            Future.successful(response)
          }
        }
      }
    }

    private def cachedAndETagNotPresent(d: Duration): IO[Future[HttpResponse]] = notCached(ttl, d)

    private def notCached(ttl: Milliseconds, d: Duration): IO[Future[HttpResponse]] = {
      doHttpRequest(headers, d).flatMap { response: HttpResponse =>
        //TODO: respect cache-control headers
        cache.set(this, response, ttl).map { _ =>
          Future.successful(response)
        }
      }
    }

    override def prepareAsync: IO[Future[HttpResponse]] = cacheResult.flatMap { cachedResponseOpt: Option[HttpResponse] =>
      cachedResponseOpt some { cachedResponse: HttpResponse =>
        cachedResponse.eTag some { eTag: String =>
          cachedAndETagPresent(cachedResponse, eTag, ttl, duration)
        } none {
          cachedAndETagNotPresent(duration)
        }
      } none {
        notCached(ttl, duration)
      }
    }
  }
}
