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
                          httpResponseCacher: HttpResponseCacher)
                         (implicit c: ExecutionContext) extends HttpClient {
  import ETagAwareHttpClient._

  override def get(u: URL, h: Headers): GetRequest = new GetRequest with CachingMixin {
    override lazy val ctx = c
    override protected val cache = httpResponseCacher
    override protected def doGetRequest: Future[HttpResponse] = httpClient.get(u, h).apply
    override protected def doHeadRequest(headers: Headers): Future[HttpResponse] = httpClient.head(u, headers).apply
    override val url = u
    override val headers = h
  }

  override def post(u: URL, h: Headers, b: RawBody): PostRequest = PostRequest(u, h, b) {
    httpClient.post(u, h, b).apply
  }

  override def put(u: URL, h: Headers, b: RawBody): PutRequest = PutRequest(u, h, b) {
    httpClient.put(u, h, b).apply
  }

  override def delete(u: URL, h: Headers): DeleteRequest = DeleteRequest(u, h) {
    httpClient.delete(u, h).apply
  }

  override def head(u: URL, h: Headers): HeadRequest = HeadRequest(u, h) {
    httpClient.head(u, h).apply
  }
}

object ETagAwareHttpClient {
  trait CachingMixin extends HttpRequest { this: GetRequest =>
    protected implicit def ctx: ExecutionContext
    protected def cache: HttpResponseCacher
    protected def doGetRequest: Future[HttpResponse]
    protected def doHeadRequest(headers: Headers): Future[HttpResponse]

    private def addIfNoneMatch(h: Headers, eTag: String): Headers = {
      h.map { headerList: HeaderList =>
        nel(HttpHeaders.IF_NONE_MATCH -> eTag, headerList.list.filterNot(_._1 === HttpHeaders.IF_NONE_MATCH))
      } orElse { Headers(HttpHeaders.IF_NONE_MATCH -> eTag) }
    }

    private def cachedAndETagPresent(cached: HttpResponse,
                                     eTag: String): Future[HttpResponse] = {
      val newHeaderList = addIfNoneMatch(this.headers, eTag)
      doHeadRequest(newHeaderList).flatMap { response: HttpResponse =>
       if(response.notModified) {
          //not modified returned - so return cached response
          Future.successful(cached)
        } else {
          val respFut = doGetRequest
          cache.set(this, respFut)
          respFut
        }
      }
    }

    private def cachedAndETagNotPresent: Future[HttpResponse] = notCached

    private def notCached: Future[HttpResponse] = {
      val respFut = doGetRequest
      cache.set(this, respFut)
      respFut
    }

    override def apply: Future[HttpResponse] = {
      cache.get(this).map { respFuture: Future[HttpResponse] =>
        respFuture.flatMap { resp: HttpResponse =>
          resp.eTag some { eTag: String =>
            cachedAndETagPresent(resp, eTag)
          } none {
            cachedAndETagNotPresent
          }
        }
      } | {
        notCached
      }
    }
  }
}
