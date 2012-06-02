package com.stackmob.newman

import java.net.URL
import request._
import request.HttpRequest._
import request.HttpRequestWithBody._
import java.util.concurrent.CopyOnWriteArrayList
import response.{HttpResponseCode, HttpResponse}
import scalaz._
import Scalaz._
import scalaz.effects._
import scalaz.concurrent._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 3:38 PM
 */

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
