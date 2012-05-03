package com.stackmob.newman.dsl

import java.net.URL
import scalaz._
import Scalaz._
import com.stackmob.newman._
import HttpClient._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.dsl
 *
 * User: aaron
 * Date: 4/27/12
 * Time: 5:20 PM
 */

object RequestBuilder {

  private val HeadersPrependLens = Lens[Headers, Option[Header]](
    get = { h: Headers => h.map(_.head) },
    set = { (headers: Headers, hOpt: Option[Header]) =>
      val listAndEltOpt: Option[(HeaderList, Header)] = headers <|*|> hOpt
      listAndEltOpt.map { tup: (HeaderList, Header) =>
        val (headerList, header) = tup
        nel(header, headerList.list)
      }
    }
  )

  case class HeaderTransformer(fn: Headers => HttpRequest) {
    def withHeader(toAdd: Header): HeaderTransformer = { h: Headers => fn(HeadersPrependLens.set(h, toAdd.some)) }
    def withHeaders(h: Headers) = fn(h)
    def withNoHeaders = fn(none[HeaderList])
  }

  case class BodyTransformer(fn: RawBody => HttpRequestWithBody) {
    def withBody(a: RawBody) = fn(a)
    def withEmptyBody = fn(EmptyRawBody)
  }

  case class HeaderAndBodyTransformer(fn: (Headers, RawBody) => HttpRequestWithBody) {
    def withBody(b: RawBody): HeaderTransformer = fn(_: Headers, b)
    def withHeader(toAdd: Header): HeaderAndBodyTransformer = { (h: Headers, b: RawBody) => fn(HeadersPrependLens.set(h, toAdd.some), b) }
    def withHeadersAndBody(h: Headers, b: RawBody) = fn(h, b)
  }

  implicit def headerFnToTransformer(fn: Headers => HttpRequest): HeaderTransformer = HeaderTransformer(fn)
  implicit def bodyFnToTransformer(fn: RawBody => HttpRequestWithBody): BodyTransformer = BodyTransformer(fn)
  implicit def headerAndBodyFnToTransformer(fn: (Headers, RawBody) => HttpRequestWithBody): HeaderAndBodyTransformer = HeaderAndBodyTransformer(fn)

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
