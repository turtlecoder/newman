package com.stackmob.newman

import request._
import request.HttpRequest._
import request.HttpRequestWithBody._
import scalaz.Lens
import scalaz.Scalaz._
import java.net.URL

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 3:18 PM
 */

object DSL {
  private val HeaderPrependLens = Lens[Headers, Option[Header]](
    get = { h: Headers => h.map(_.head) },
    set = { (headers: Headers, hOpt: Option[Header]) =>
      (headers, hOpt) match {
        case (Some(h1), Some(h2)) => Some(nel(h2, h1.list))
        case (Some(h1), None) => Some(h1)
        case (None, Some(h2)) => Some(nel(h2))
        case (None, None) => None
      }
    }
  )

  private val HeadersPrependLens = Lens[Headers, Headers](
    get = {h: Headers => h },
    set = { (existing: Headers, toPrepend: Headers) =>
      (existing, toPrepend) match {
        case (Some(e), Some(t)) => Some(t :::> e.list)
        case (Some(e), None) => Some(e)
        case (None, Some(t)) => Some(t)
        case (None, None) => Option.empty[HeaderList]
      }
    }
  )

  private val BodyPrependLens = Lens[Array[Byte], Array[Byte]](
    get = { b: Array[Byte] => b },
    set = { (existing: Array[Byte], toPrepend: Array[Byte]) => toPrepend ++ existing }
  )

  sealed trait Transformer {
    protected type T <: Transformer

    def toRequest: HttpRequest
    def headers: Headers = none
    def addHeader(toAdd: Header): T
    def addHeaders(toAdd: Headers): T
  }

  case class HeaderTransformer(fn: Headers => HttpRequest, override val headers: Headers = none)
    extends Transformer {
    override type T = HeaderTransformer

    override def addHeader(toAdd: Header) = HeaderTransformer(fn, HeaderPrependLens.set(headers, toAdd.some))
    override def addHeaders(toAdd: Headers) = HeaderTransformer(fn, HeadersPrependLens.set(headers, toAdd))
    override def toRequest = fn(headers)
  }

  case class HeaderAndBodyTransformer(fn: (Headers, RawBody) => HttpRequestWithBody,
                                      override val headers: Headers = none,
                                      body: RawBody = EmptyRawBody)
    extends Transformer {

    override type T = HeaderAndBodyTransformer
    def addBody(b: RawBody) = HeaderAndBodyTransformer(fn, headers, BodyPrependLens.set(body, b))
    override def addHeader(toAdd: Header) = HeaderAndBodyTransformer(fn, HeaderPrependLens.set(headers, toAdd.some), body)
    override def addHeaders(toAdd: Headers) = HeaderAndBodyTransformer(fn, HeadersPrependLens.set(headers, toAdd), body)
    override def toRequest = fn(headers, body)
  }

  //implicit that goes from HeaderTransformer to HttpRequest
  implicit def headerTransformerToHttpRequest(h: HeaderTransformer) = h.fn(h.headers)
  implicit def headerAndBodyTransformerToHttpRequest(h: HeaderAndBodyTransformer) = h.fn(h.headers, h.body)


  def GET(url: URL)(implicit client: HttpClient) = HeaderTransformer { h: Headers =>
    client.get(url, h)
  }

  def PUT(url: URL)(implicit client: HttpClient) = HeaderAndBodyTransformer { (h: Headers, b: RawBody) =>
    client.put(url, h, b)
  }

  def POST(url: URL)(implicit client: HttpClient) = HeaderAndBodyTransformer { (h: Headers, b: RawBody) =>
    client.post(url, h, b)
  }

  def DELETE(url: URL)(implicit client: HttpClient) = HeaderTransformer { h: Headers =>
    client.delete(url, h)
  }

  def HEAD(url: URL)(implicit client: HttpClient) = HeaderTransformer { h: Headers =>
    client.head(url, h)
  }
}
