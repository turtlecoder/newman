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

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 3:38 PM
 */

class DummyHttpClient extends HttpClient {
  import DummyHttpClient._

  val getRequests = new CopyOnWriteArrayList[(URL, Headers)]()
  val postRequests = new CopyOnWriteArrayList[(URL, Headers, RawBody)]()
  val putRequests = new CopyOnWriteArrayList[(URL, Headers, RawBody)]()
  val deleteRequests = new CopyOnWriteArrayList[(URL, Headers)]()
  val headRequests = new CopyOnWriteArrayList[(URL, Headers)]()

  def get(url: URL, headers: Headers): GetRequest = {
    getRequests.add(url -> headers)
    DummyGetRequest(url, headers)
  }

  def post(url: URL, headers: Headers, body: RawBody): PostRequest = {
    postRequests.add((url, headers, body))
    DummyPostRequest(url, headers, body)
  }

  def put(url: URL, headers: Headers, body: RawBody): PutRequest = {
    putRequests.add((url, headers, body))
    DummyPutRequest(url, headers, body)
  }

  def delete(url: URL, headers: Headers): DeleteRequest = {
    deleteRequests.add(url -> headers)
    DummyDeleteRequest(url, headers)
  }

  def head(url: URL, headers: Headers): HeadRequest = {
    headRequests.add(url -> headers)
    DummyHeadRequest(url, headers)
  }
}

object DummyHttpClient {
  trait DummyExecutor extends HttpRequest {
    override def execute = HttpResponse(HttpResponseCode.Ok, none, EmptyRawBody).pure[IO]
  }

  case class DummyGetRequest(override val url: URL, override val headers: Headers) extends GetRequest with DummyExecutor
  case class DummyPostRequest(override val url: URL, override val headers: Headers, override val body: RawBody) extends PostRequest with DummyExecutor
  case class DummyPutRequest(override val url: URL, override val headers: Headers, override val body: RawBody) extends PutRequest with DummyExecutor
  case class DummyDeleteRequest(override val url: URL, override val headers: Headers) extends DeleteRequest with DummyExecutor
  case class DummyHeadRequest(override val url: URL, override val headers: Headers) extends HeadRequest with DummyExecutor
}
