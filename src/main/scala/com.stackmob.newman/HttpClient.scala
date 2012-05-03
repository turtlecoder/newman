package com.stackmob.newman

import com.stackmob.newman.response.HttpResponse
import scalaz._
import scalaz.effects._
import java.net.URL
import HttpClient._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.barney.service.filters.proxy
 *
 * User: aaron
 * Date: 4/24/12
 * Time: 4:58 PM
 */



trait HttpRequest {
  def url: URL
  def headers: Headers
  def execute: IO[HttpResponse]
}

sealed trait HttpRequestWithBody extends HttpRequest {
  def body: RawBody
}

sealed trait HttpRequestWithoutBody extends HttpRequest

trait GetRequest extends HttpRequestWithoutBody
trait PostRequest extends HttpRequestWithBody
trait PutRequest extends HttpRequestWithBody
trait DeleteRequest extends HttpRequestWithoutBody
trait HeadRequest extends HttpRequestWithoutBody

trait HttpClient {
  def get(url: URL, headers: Headers): GetRequest
  def post(url: URL, headers: Headers, body: RawBody): PostRequest
  def put(url: URL, headers: Headers, body: RawBody): PutRequest
  def delete(url: URL, headers: Headers): DeleteRequest
  def head(url: URL, headers: Headers): HeadRequest
}

object HttpClient {
  type Header = (String, String)
  type HeaderList = NonEmptyList[Header]
  type Headers = Option[HeaderList]
  type RawBody = Array[Byte]
  val EmptyRawBody = Array[Byte]()
}
