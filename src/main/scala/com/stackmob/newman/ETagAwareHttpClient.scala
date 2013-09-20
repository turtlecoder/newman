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
import com.stackmob.newman.concurrent.RichTwitterFuture
import response.HttpResponse
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import ETagAwareHttpClient._
import scalaz.NonEmptyList._
import org.apache.http.HttpHeaders
import com.twitter.concurrent.AsyncMutex
import java.util.concurrent.ConcurrentHashMap

/**
 * an HttpClient that respects ETag headers and caches {{{HttpResponse}}}s appropriately
 * @param httpClient the underlying HttpClient to do network requests when appropriate
 * @param httpResponseCacher the cacher to cache {{{HttpResponse}}}s when appropriate
 * @param ctx the execution context to handle future scheduling for acquiring and releasing cache line access
 */
class ETagAwareHttpClient(httpClient: HttpClient,
                          httpResponseCacher: HttpResponseCacher)
                         (implicit ctx: ExecutionContext) extends HttpClient {

  /**
   * a concurrent map of {{{com.twitter.concurrent.AsyncMutex}}}es that are used to synchronize access
   * to each cache line, to avoid race conditions on each cache interaction in calls to get
   */
  private val cacheLineMutexes = new ConcurrentHashMap[GetRequest, AsyncMutex]()

  override def get(u: URL, h: Headers): GetRequest = new GetRequest {

    override val url = u
    override val headers = h

    private def applyImpl(respFuture: Future[HttpResponse]): Future[HttpResponse] = {
      respFuture.flatMap { resp =>
        resp.eTag.map { eTag =>
        //the response was cached and has an eTag so check it against the server
          val newHeaderList = addIfNoneMatch(resp.headers, eTag)
          httpClient.get(u, newHeaderList).apply.flatMap { response: HttpResponse =>
            if(response.notModified) {
              //the response was not modified, so return the cached response
              Future.successful(resp)
            } else {
              //the response was modified, so remove from the cache and run again against the server as normal
              httpResponseCacher.remove(this) //this call immediately removes from the cache
              httpResponseCacher.apply(this) //this call fills the cache as normal
            }
          }
        } | {
          //the response was cached and has no eTag, so execute the request as normal
          httpResponseCacher.apply(this)
        }
      }
    }

    override def apply: Future[HttpResponse] = {
      val newMutex = new AsyncMutex()
      val mutex = Option(cacheLineMutexes.putIfAbsent(this, newMutex)).getOrElse(newMutex)
      mutex.acquire().toScalaFuture.flatMap { permit =>
        val fut = httpResponseCacher.get(this).map(applyImpl).getOrElse {
          httpResponseCacher.apply(this)
        }
        fut.onComplete { _ =>
          permit.release()
        }
        fut
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
