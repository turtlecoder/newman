package com.stackmob.newman

import scalaz.effects._
import java.net.URL

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
  def body: Array[Byte]
}

sealed trait HttpRequestWithoutBody extends HttpRequest

trait GetRequest extends HttpRequestWithoutBody
trait PostRequest extends HttpRequestWithBody
trait PutRequest extends HttpRequestWithBody
trait DeleteRequest extends HttpRequestWithoutBody
trait HeadRequest extends HttpRequestWithoutBody

trait HttpClient {
  def get(url: URL, headers: Headers): GetRequest
  def post(url: URL, headers: Headers, body: Array[Byte]): PostRequest
  def put(url: URL, headers: Headers, body: Array[Byte]): PutRequest
  def delete(url: URL, headers: Headers): DeleteRequest
  def head(url: URL, headers: Headers): HeadRequest
}
