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

import caching._
import request._
import java.net.URL

class ReadCachingHttpClient(httpClient: HttpClient,
                            httpResponseCacher: HttpResponseCacher) extends HttpClient {

  import ReadCachingHttpClient._
  override def get(u: URL, h: Headers): GetRequest = new ReadCachingGetRequest(u, h, httpResponseCacher)

  override def post(u: URL, h: Headers, b: RawBody): PostRequest = PostRequest(u, h, b) {
    httpClient.post(u, h, b).apply
  }

  override def put(u: URL, h: Headers, b: RawBody): PutRequest = PutRequest(u, h, b) {
    httpClient.put(u, h, b).apply
  }

  override def delete(u: URL, h: Headers): DeleteRequest = DeleteRequest(u, h) {
    httpClient.delete(u, h).apply
  }

  override def head(u: URL, h: Headers): HeadRequest = new ReadCachingHeadRequest(u, h, httpResponseCacher)
}

object ReadCachingHttpClient {
  private[ReadCachingHttpClient] class ReadCachingGetRequest(override val url: URL,
                                                             override val headers: Headers,
                                                             cacher: HttpResponseCacher) extends GetRequest {
    override def apply = {
      cacher.apply(this)
    }
  }

  private[ReadCachingHttpClient] class ReadCachingHeadRequest(override val url: URL,
                                                              override val headers: Headers,
                                                              cacher: HttpResponseCacher) extends HeadRequest {
    override def apply = {
      cacher.apply(this)
    }
  }
}
