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
    override protected def doHeadRequest(headers: Headers): Future[HttpResponse] = {
      httpClient.head(u, headers).apply
    }
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
    protected def doHeadRequest(headers: Headers): Future[HttpResponse]

    private def addIfNoneMatch(h: Headers, eTag: String): Headers = {
      h.map { headerList: HeaderList =>
        nel(HttpHeaders.IF_NONE_MATCH -> eTag, headerList.list.filterNot(_._1 === HttpHeaders.IF_NONE_MATCH))
      } orElse { Headers(HttpHeaders.IF_NONE_MATCH -> eTag) }
    }

    /**
     * check a cached response's eTag against the server using a head request
     * @param cached the cached response
     * @param eTag the cached response's eTag
     * @return the resulting HttpResponse. if the eTag is up to date, this is the same as {{{cached}}}. otherwise, the result of running the request against the server
     */
    private def checkETag(cached: HttpResponse,
                          eTag: String): Future[HttpResponse] = {
      val newHeaderList = addIfNoneMatch(this.headers, eTag)
      doHeadRequest(newHeaderList).flatMap { response: HttpResponse =>
        if(response.notModified) {
          //the response was not modified, so return the cached response
          Future.successful(cached)
        } else {
          //the response was modified, so remove it from the cache and rerun the request.
          //don't care if the response existed or, if it did, when the response returned,
          //just make sure that after this line executes it's gone
          cache.remove(this)
          cache.apply(this)
        }
      }
    }


    override def apply: Future[HttpResponse] = {
      cache.get(this).map { respFut =>
        respFut.flatMap { resp =>
          resp.eTag.map { eTag: String =>
            //the response was found in the cache and it has an eTag so check the it against the server
            checkETag(resp, eTag)
          } | {
            //the response was found in the cache and it doesn't have an eTag, so just do the request as normal
            cache.apply(this)
          }
        }
      } | {
        //the response was not found in the cache, so just do the request as normal
        cache.apply(this)
      }
    }
  }
}
