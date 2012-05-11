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
  implicit val client = new ApacheHttpClient

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

  sealed trait Builder {
    protected type T <: Builder

    def toRequest: HttpRequest
    def headers: Headers = none
    def addHeader(toAdd: Header): T
    def addHeaders(toAdd: Headers): T
  }

  case class HeaderBuilder(fn: Headers => HttpRequest, override val headers: Headers = none)
    extends Builder {

    override type T = HeaderBuilder

    override def addHeader(toAdd: Header) = HeaderBuilder(fn, HeaderPrependLens.set(headers, toAdd.some))
    override def addHeaders(toAdd: Headers) = HeaderBuilder(fn, HeadersPrependLens.set(headers, toAdd))
    override def toRequest = fn(headers)
  }

  case class HeaderAndBodyBuilder(fn: (Headers, RawBody) => HttpRequestWithBody,
                                      override val headers: Headers = none,
                                      body: RawBody = RawBody.empty)
    extends Builder {

    override type T = HeaderAndBodyBuilder
    def addBody(b: RawBody) = HeaderAndBodyBuilder(fn, headers, BodyPrependLens.set(body, b))
    override def addHeader(toAdd: Header) = HeaderAndBodyBuilder(fn, HeaderPrependLens.set(headers, toAdd.some), body)
    override def addHeaders(toAdd: Headers) = HeaderAndBodyBuilder(fn, HeadersPrependLens.set(headers, toAdd), body)
    override def toRequest = fn(headers, body)
  }

  implicit def transformerToHttpRequest(t: Builder) = t.toRequest


  def GET(url: URL)(implicit client: HttpClient) = HeaderBuilder { h: Headers =>
    client.get(url, h)
  }

  def PUT(url: URL)(implicit client: HttpClient) = HeaderAndBodyBuilder { (h: Headers, b: RawBody) =>
    client.put(url, h, b)
  }

  def POST(url: URL)(implicit client: HttpClient) = HeaderAndBodyBuilder { (h: Headers, b: RawBody) =>
    client.post(url, h, b)
  }

  def DELETE(url: URL)(implicit client: HttpClient) = HeaderBuilder { h: Headers =>
    client.delete(url, h)
  }

  def HEAD(url: URL)(implicit client: HttpClient) = HeaderBuilder { h: Headers =>
    client.head(url, h)
  }
}
