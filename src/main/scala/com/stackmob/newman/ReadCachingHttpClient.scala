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

import scalaz.Scalaz._
import caching._
import request._
import response.HttpResponse
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class ReadCachingHttpClient(httpClient: HttpClient,
                            httpResponseCacher: HttpResponseCacher)
                           (implicit c: ExecutionContext) extends HttpClient {
  import ReadCachingHttpClient._

  override def get(u: URL, h: Headers): GetRequest = new GetRequest with CachingMixin {
    override protected lazy val ctx = c
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers): Future[HttpResponse] = {
      httpClient.get(u, h).apply
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

  override def head(u: URL, h: Headers): HeadRequest = new HeadRequest with CachingMixin {
    override protected lazy val ctx = c
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers): Future[HttpResponse] = {
      httpClient.head(u, h).apply
    }
    override val url = u
    override val headers = h
  }
}

object ReadCachingHttpClient {
  trait CachingMixin { this: HttpRequest =>
    protected implicit def ctx: ExecutionContext
    protected def cache: HttpResponseCacher
    protected def doHttpRequest(headers: Headers): Future[HttpResponse]

    override def apply: Future[HttpResponse] = {
      cache.get(this) | {
        val respFut = doHttpRequest(headers)
        cache.set(this, respFut)
        respFut
      }
    }
  }
}
