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
import scalaz._
import Scalaz._
import com.stackmob.newman.caching.HttpResponseCacher
import response.HttpResponse
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import ETagAwareHttpClient._
import scalaz.NonEmptyList._
import org.apache.http.HttpHeaders

/**
 * an HttpClient that respects ETag headers and caches {{{HttpResponse}}}s appropriately
 * @param httpClient the underlying HttpClient to do network requests when appropriate
 * @param httpResponseCacher the cacher to cache {{{HttpResponse}}}s when appropriate
 * @param c the execution context to handle future scheduling
 */
class ETagAwareHttpClient(httpClient: HttpClient,
                          httpResponseCacher: HttpResponseCacher)
                         (implicit c: ExecutionContext) extends HttpClient {

  override def get(u: URL, h: Headers): GetRequest = new GetRequest {

    override val url = u
    override val headers = h

    override def apply: Future[HttpResponse] = {
      httpResponseCacher.remove(this).map { cachedRespFut =>
        cachedRespFut.flatMap { cachedResp =>
          cachedResp.eTag.map { eTag =>
          //the response was cached and has an eTag so check it against the server
            val newHeaderList = addIfNoneMatch(cachedResp.headers, eTag)
            httpClient.get(u, newHeaderList).apply.flatMap { response: HttpResponse =>
              if(response.notModified) {
                //the response was not modified, so return the cached response
                Future.successful(cachedResp)
              } else {
                //the response was modified, so run it against the server as normal
                httpResponseCacher.apply(this)
              }
            }
          } | {
            //the response was cached and has no eTag, so execute the request as normal
            httpResponseCacher.apply(this)
          }
        }
      } | {
        //response was not cached, so execute the request and cache it
        httpResponseCacher.apply(this)
      }
    }
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
  /**
   * add an If-None-Match header to the given headers, using the given eTag
   * @param eTag the eTag to set in the new If-None-Match headers
   * @return the new headers
   */
  private[ETagAwareHttpClient] def addIfNoneMatch(existingHeaders: Headers, eTag: String): Headers = {
    existingHeaders.map { headerList: HeaderList =>
      nel(HttpHeaders.IF_NONE_MATCH -> eTag, headerList.list.filterNot(_._1 === HttpHeaders.IF_NONE_MATCH))
    }.orElse {
      Headers(HttpHeaders.IF_NONE_MATCH -> eTag)
    }
  }

}
