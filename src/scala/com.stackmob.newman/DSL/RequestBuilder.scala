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

  case class HeaderTransformer(fn: Headers => HttpRequest) {
    def withHeaders(h: Headers) = fn(h)
  }

  case class BodyTransformer(fn: Array[Byte] => HttpRequestWithBody) {
    def withBody(a: Array[Byte]) = fn(a)
  }

  case class HeaderAndBodyTransformer(fn: (Headers, Array[Byte]) => HttpRequestWithBody) {
    def withBody(b: Array[Byte]) = fn(_: Headers, b)
    def withHeaders(h: Headers) = fn(h, _: Array[Byte])
    def withHeadersAndBody(h: Headers, b: Array[Byte]) = fn(h, b)
  }

  implicit def headerFnToTransformer(fn: Headers => HttpRequest) =
    HeaderTransformer(fn)
  implicit def bodyFnToTransformer(fn: Array[Byte] => HttpRequestWithBody) =
    BodyTransformer(fn)
  implicit def headerAndBodyFnToTransformer(fn: (Headers, Array[Byte]) => HttpRequestWithBody) =
    HeaderAndBodyTransformer(fn)

  def GET(url: URL)(implicit client: HttpClient) = { h: Headers => client.get(url, h) }
  def PUT(url: URL)(implicit client: HttpClient) = { (h: Headers, b: Array[Byte]) => client.put(url, h, b) }
  def POST(url: URL)(implicit client: HttpClient) = { (h: Headers, b: Array[Byte]) => client.post(url, h, b) }
  def DELETE(url: URL)(implicit client: HttpClient) = { (h: Headers) => client.delete(url, h) }
  def HEAD(url: URL)(implicit client: HttpClient) = { (h: Headers) => client.head(url, h) }
}
