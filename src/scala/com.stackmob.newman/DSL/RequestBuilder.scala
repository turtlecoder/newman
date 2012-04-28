package com.stackmob.newman.DSL

import java.net.URL
import com.stackmob.newman._
import scalaz._
import Scalaz._
import com.stackmob.common.validation.validating

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.DSL
 *
 * User: aaron
 * Date: 4/27/12
 * Time: 5:20 PM
 */

object RequestBuilder {

  private case class HeaderTransformer(fn: Headers => HttpRequest) {
    def withHeaders(h: Headers) = fn(h)
    def withNoHeaders = fn(none[HeaderList])
  }

  private case class BodyTransformer(fn: RawBody => HttpRequestWithBody) {
    def withBody(a: RawBody) = fn(a)
    def withEmptyBody = fn(EmptyRawBody)
  }

  private case class HeaderAndBodyTransformer(fn: (Headers, RawBody) => HttpRequestWithBody) {
    def withBody(b: RawBody): HeaderTransformer = fn(_: Headers, b)
    def withNoHeaders: BodyTransformer = fn(none, _: RawBody)
    def withHeaders(h: Headers): BodyTransformer = fn(h, _: RawBody)
    def withHeadersAndBody(h: Headers, b: RawBody) = fn(h, b)
  }

  implicit def headerFnToTransformer(fn: Headers => HttpRequest) = HeaderTransformer(fn)
  implicit def bodyFnToTransformer(fn: RawBody => HttpRequestWithBody) = BodyTransformer(fn)
  implicit def headerAndBodyFnToTransformer(fn: (Headers, RawBody) => HttpRequestWithBody) = HeaderAndBodyTransformer(fn)

  def GET(url: URL)(implicit client: HttpClient): Headers => GetRequest = { h: Headers =>
    client.get(url, h)
  }

  def PUT(url: URL)(implicit client: HttpClient): (Headers, RawBody) => PutRequest = { (h: Headers, b: RawBody) =>
    client.put(url, h, b)
  }

  def POST(url: URL)(implicit client: HttpClient): (Headers, RawBody) => PostRequest = { (h: Headers, b: RawBody) =>
    client.post(url, h, b)
  }

  def DELETE(url: URL)(implicit client: HttpClient): Headers => DeleteRequest = { h: Headers =>
    client.delete(url, h)
  }

  def HEAD(url: URL)(implicit client: HttpClient): Headers => HeadRequest = { h: Headers =>
    client.head(url, h)
  }
}
