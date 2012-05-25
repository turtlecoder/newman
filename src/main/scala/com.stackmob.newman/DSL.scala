package com.stackmob.newman

import request._
import request.HttpRequest._
import request.HttpRequestWithBody._
import java.net.URL
import scalaz._
import Scalaz._
import net.liftweb.json._

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

    def addHeaders(toAdd: Headers): T
    def addHeaders(toAdd: Header): T
    def addHeaders(h: Header, tail: Header*): T
    def addHeaders(toAdd: HeaderList): T
    def addHeaders(toAdd: List[Header]): T

    def setHeaders(toSet: Headers): T
    def setHeaders(toSet:Header): T
    def setHeaders(h: Header, tail: Header*): T
    def setHeaders(toSet: HeaderList): T
    def setHeaders(toSet: List[Header]): T
  }

  case class HeaderBuilder(fn: Headers => HttpRequest, override val headers: Headers = none)
    extends Builder {

    override type T = HeaderBuilder

    override def addHeaders(toAdd: Headers) = HeaderBuilder(fn, HeadersPrependLens.set(headers, toAdd))
    override def addHeaders(h: Header, tail: Header*)= addHeaders(Headers(h, tail:_*))
    override def addHeaders(toAdd: HeaderList) = addHeaders(Headers(toAdd))
    override def addHeaders(toAdd: List[Header]) = addHeaders(Headers(toAdd))

    override def setHeaders(toSet: Headers) = HeaderBuilder(fn, toSet)
    override def setHeaders(toSet:Header) = setHeaders(Headers(toSet))
    override def setHeaders(h: Header, tail: Header*) = setHeaders(Headers(h, tail:_*))
    override def setHeaders(toSet: HeaderList) = setHeaders(Headers(toSet))
    override def setHeaders(toSet: List[Header]) = setHeaders(Headers(toSet))
    override def addHeaders(toAdd: Header) = addHeaders(Headers(toAdd))

    override def toRequest = fn(headers)
  }

  case class HeaderAndBodyBuilder(fn: (Headers, RawBody) => HttpRequestWithBody,
                                      override val headers: Headers = none,
                                      body: RawBody = RawBody.empty)
    extends Builder {

    override type T = HeaderAndBodyBuilder
    def addBody(b: RawBody) = HeaderAndBodyBuilder(fn, headers, BodyPrependLens.set(body, b))
    def setBody(b: RawBody) = HeaderAndBodyBuilder(fn, headers, b)

    //todo: add default writer
    import net.liftweb.json.scalaz.JsonScalaz._
    def setBody[A <: AnyRef](value: A)(implicit writer: JSONW[A]) = HeaderAndBodyBuilder(fn, headers, compact(render(toJSON(value))).getBytes(com.stackmob.newman.Constants.UTF8Charset))

    override def addHeaders(toAdd: Headers) = HeaderAndBodyBuilder(fn, HeadersPrependLens.set(headers, toAdd), body)
    override def addHeaders(toAdd: HeaderList) = addHeaders(Headers(toAdd))
    override def addHeaders(toAdd: List[Header]) = addHeaders(Headers(toAdd))
    override def addHeaders(toAdd: Header) = addHeaders(Headers(toAdd))
    override def addHeaders(h: Header, tail: Header*) = addHeaders(Headers(h, tail:_*))

    override def setHeaders(toSet: Headers) = HeaderAndBodyBuilder(fn, toSet, body)
    override def setHeaders(toSet:Header) = setHeaders(Headers(toSet))
    override def setHeaders(h: Header, tail: Header*) = setHeaders(Headers(h, tail:_*))
    override def setHeaders(toSet: HeaderList) = setHeaders(Headers(toSet))
    override def setHeaders(toSet: List[Header]) = setHeaders(Headers(toSet))

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
