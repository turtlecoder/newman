package com.stackmob.newman

import request._
import request.HttpRequest._
import request.HttpRequestWithBody._
import com.stackmob.newman.Constants._
import java.net.URL
import response.HttpResponse.JSONParsingError
import response.{HttpResponse, HttpResponseCode}
import scalaz._
import Scalaz._
import effects.IO
import net.liftweb.json.scalaz.JsonScalaz._
import net.liftweb.json._
import serialization.common.DefaultBodySerialization
import java.nio.charset.Charset
import com.stackmob.common.util.casts._
import com.stackmob.common.validation._
import com.stackmob.common.validation.ThrowableValidation

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 3:18 PM
 */

object DSL extends URLBuilderDSL {

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
    def addBody(b: RawBody): HeaderAndBodyBuilder = HeaderAndBodyBuilder(fn, headers, BodyPrependLens.set(body, b))
    def addBody(s: String)(implicit charset: Charset = UTF8Charset): HeaderAndBodyBuilder = addBody(s.getBytes(charset))
    def setBody(b: RawBody): HeaderAndBodyBuilder = HeaderAndBodyBuilder(fn, headers, b)
    def setBodyString(s: String)(implicit charset: Charset = UTF8Charset): HeaderAndBodyBuilder = setBody(s.getBytes(charset))

    import net.liftweb.json.scalaz.JsonScalaz._
    def setBody[A <: AnyRef](value: A)
                            (implicit writer: JSONW[A] = DefaultBodySerialization.getWriter[A],
                             charset: Charset = UTF8Charset): HeaderAndBodyBuilder = {
      //if it's a string, don't JSON encode it
      val bodyString = value.cast[String].map(s => s) | compact(render(toJSON(value)))
      setBodyString(bodyString)
    }

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

   /* Response Handler DSL */

  /**
   * Facilitates the handling of various response codes per response, where each handler for a given code may fail or may succeed with
   * a value of some type `T`. `ResponseHandler` is an immutable wrapper; handlers, a `HTTPResponse => Validation[Throwable,T].
   * can be added by using handleCode`, which returns a new instance. After all desired handlers have been added calling `sealHandlers`
   * or using the implicit provided in [[com.stackmob.newman.DSL]] will return an `IO[Validation[Throwable,T]`.
   *
   * For response codes without a "catchall" handler always `scalaz.Failure` with a [[com.stackmob.newman.DSL.UnhandledResponseCode]]
   * as its value.
   *
   * To begin using `ResponseHandler` call [[com.stackmob.newman.request.HttpRequest]]'s `prepare` method and then begin changing
   * calls to `handleCode`.
   *
   * Example:
   *
   * {{{
   *   GET(genURL("cnames", cName))
   *    .addHeaders(acceptHeader)
   *    .prepare
   *    .handleCode(HttpResponseCode.Ok, (resp: HttpResponse) => {
   *      val json = parse(resp.bodyString)
   *      (json \ "app-id", json \ "env") match {
   *        case (JInt(appID), JString(env)) => (appID.longValue(), env.toEnum[EnvironmentType]).some.success[Throwable]
   *         case _ => (UnexpectedLookupBody(resp.bodyString): Throwable).fail[Option[(Long,EnvironmentType)]]
   *       }
   *     })
   *    .handleCode(HttpResponseCode.NotFound, (_: HttpResponse) => none.success)
   *    .handleCode(HttpResponseCode.InternalServerError, (resp: HttpResponse) => {
   *      (new Exception("server error: %s" format resp.bodyString)).fail
   *    })
   *    .sealHandlers // this can typically be omitted
   * }}}
   *
   * If the response is expected to be considered a success when it its code is `200 OK`
   * and is expected to have a body whose content can is valid JSON `expectJSONBody`
   * can be called given there is an implicit [[net.liftweb.json.scalaz.Types.JSONR]] in scope for `T`.
   * See `expectJSONBody` for more info.
   *
   * Example:
   *
   * {{{
   *   implicit def jsonrForBody: JSONR[(Long,EnvironmentType) = ...
   *   GET(genURL("cnames", cName))
   *    .addHeaders(acceptHeader)
   *    .prepare
   *    .expectJSONBody[(Long,EnvironmentType)] // if called in any other position in the chain the type parameter should not need to be specified
   *    .handleCode(HttpResponseCode.NotFound, (_: HttpResponse) => none.success)
   *
   * }}}
   *
   * If the response is expected to be considered a success when it its code is `204 No Content`,
   * `expectNoContent(t: T)` can be called to return a `scalaz.Success` when the `IO` is performed.
   * See `expectNoContent` for more info.
   *
   */
   case class ResponseHandler[T](handlers: List[(HttpResponseCode, HttpResponse => ThrowableValidation[T])], respIO: IO[HttpResponse]) {

     /**
      * Adds a handler (a function that is called when the given code is matched) and returns a new ResponseHandler
      * @param code response code this handler is for
      * @param handler function to call when response with given code is encountered
      * @return
      */
     def handleCode(code: HttpResponseCode, handler: HttpResponse => ThrowableValidation[T]): ResponseHandler[T] =
       copy(handlers = (code,handler) :: handlers)

     /**
      * Adds a handler that expects a 200 response and a JSON body readable by
      * a JSONR for the type of this ResponseHandler. A Response can only return
      * one successful type per request so multiple calls to this method should not be
      * made, however, if there are there is no effect on the handling of the response.
      * @return a new [[com.stackmob.newman.DSL.ResponseHandler]]
      */
     def expectJSONBody(implicit reader: JSONR[T],
                        charset: Charset = UTF8Charset): ResponseHandler[T] = {
       handleCode(HttpResponseCode.Ok, (resp: HttpResponse) => resp.bodyAs[T].mapFailure(JSONParsingError(_): Throwable))
     }


     /**
      * Adds a handler that expects a 204 response. If encountered, the value passed to this method
      * will be returned when the handler is sealed and the IO is performed. This method should
      * only be called once per response but multiple calls have no effect
      * @param successValue - the value to return successfully when a 204 is encountered
      * @return a new ResponseHandler
      */
     def expectNoContent(successValue: T) = {
       handleCode(HttpResponseCode.NoContent, (_: HttpResponse) => successValue.success)
     }

     def sealHandlers: IO[ThrowableValidation[T]] = {
       respIO.map{ response =>
         handlers.find(_._1 === response.code).map(_._2 apply response) | UnhandledResponseCode(response.code).fail[T]
       }
     }
  }

  trait IOResponseW extends NewType[IO[HttpResponse]] {
    def handleCode[T](code: HttpResponseCode, handler: HttpResponse => ThrowableValidation[T]) =
      ResponseHandler((code, handler) :: Nil, value)

    def expectJSONBody[T](code: HttpResponseCode)
                                (implicit reader: JSONR[T], charset: Charset = UTF8Charset) = {
      handleCode(code, (resp: HttpResponse) => resp.bodyAs[T].mapFailure(JSONParsingError(_): Throwable))
    }
  }

  case class UnhandledResponseCode(code: HttpResponseCode)
    extends Exception("undhandled response code %d" format code.code)


  implicit def ioRespToW(ioResp: IO[HttpResponse]): IOResponseW = new IOResponseW {
    val value = ioResp
  }

  implicit def ResponseHandlerToResponse[T](handler: ResponseHandler[T]): IO[ThrowableValidation[T]] = handler.sealHandlers
  /* End Response Handler DSL */



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
