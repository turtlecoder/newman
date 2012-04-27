package com.stackmob.newman

import scalaz._
import Scalaz._
import scalaz.effects._
import org.jboss.netty.handler.codec.http.HttpResponseStatus
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

trait HttpClientComponent {

  type HeaderList = Option[NonEmptyList[(String, String)]]

  case class HttpResponse(code: HttpResponseStatus, headers: HeaderList, body: Array[Byte])

  sealed trait HttpRequest {
    def url: URL
    def headers: HeaderList

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
    def get(url: URL, headers: HeaderList): GetRequest
    def post(url: URL, headers: HeaderList, body: Array[Byte]): PostRequest
    def put(url: URL, headers: HeaderList, body: Array[Byte]): PutRequest
    def delete(url: URL, headers: HeaderList): DeleteRequest
    def head(url: URL, headers: HeaderList): HeadRequest
  }

  def httpClient: HttpClient
}