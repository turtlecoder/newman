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

import java.net.URL
import com.stackmob.newman._
import com.stackmob.newman.request._
import com.stackmob.newman.response._
import java.util.concurrent.CopyOnWriteArrayList
import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.concurrent._

class DummyHttpClient(val responseToReturn: () => HttpResponse = () => DummyHttpClient.CannedResponse) extends HttpClient {
  import DummyHttpClient._

  val getRequests = new CopyOnWriteArrayList[(URL, Headers)]()
  val postRequests = new CopyOnWriteArrayList[(URL, Headers, RawBody)]()
  val putRequests = new CopyOnWriteArrayList[(URL, Headers, RawBody)]()
  val deleteRequests = new CopyOnWriteArrayList[(URL, Headers)]()
  val headRequests = new CopyOnWriteArrayList[(URL, Headers)]()

  def totalNumRequestsMade = getRequests.size + postRequests.size + putRequests.size + deleteRequests.size + headRequests.size

  def get(url: URL, headers: Headers): GetRequest = {
    getRequests.add(url -> headers)
    DummyGetRequest(url, headers, responseToReturn)
  }

  def post(url: URL, headers: Headers, body: RawBody): PostRequest = {
    postRequests.add((url, headers, body))
    DummyPostRequest(url, headers, body, responseToReturn)
  }

  def put(url: URL, headers: Headers, body: RawBody): PutRequest = {
    putRequests.add((url, headers, body))
    DummyPutRequest(url, headers, body, responseToReturn)
  }

  def delete(url: URL, headers: Headers): DeleteRequest = {
    deleteRequests.add(url -> headers)
    DummyDeleteRequest(url, headers, responseToReturn)
  }

  def head(url: URL, headers: Headers): HeadRequest = {
    headRequests.add(url -> headers)
    DummyHeadRequest(url, headers, responseToReturn)
  }
}

object DummyHttpClient {
  val CannedResponse = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
  trait DummyExecutor extends HttpRequest { this: HttpRequest =>
    def responseToReturn: () => HttpResponse
    override def prepareAsync = responseToReturn().pure[Promise].pure[IO]
  }

  case class DummyGetRequest(override val url: URL,
                             override val headers: Headers,
                             override val responseToReturn: () => HttpResponse) extends GetRequest with DummyExecutor

  case class DummyPostRequest(override val url: URL,
                              override val headers: Headers,
                              override val body: RawBody,
                              override val responseToReturn: () => HttpResponse) extends PostRequest with DummyExecutor

  case class DummyPutRequest(override val url: URL,
                             override val headers: Headers,
                             override val body: RawBody,
                             override val responseToReturn: () => HttpResponse) extends PutRequest with DummyExecutor

  case class DummyDeleteRequest(override val url: URL,
                                override val headers: Headers,
                                override val responseToReturn: () => HttpResponse) extends DeleteRequest with DummyExecutor

  case class DummyHeadRequest(override val url: URL,
                              override val headers: Headers,
                              override val responseToReturn: () => HttpResponse) extends HeadRequest with DummyExecutor
}
