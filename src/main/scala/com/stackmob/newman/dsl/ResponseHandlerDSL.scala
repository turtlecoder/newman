package com.stackmob.newman
package dsl

import scalaz._
import Scalaz._
import scalaz.effects.IO
import com.stackmob.common.validation._
import response.HttpResponseCode
import response.HttpResponse
import response.HttpResponse.JSONParsingError
import java.nio.charset.Charset
import Constants._
import net.liftweb.json.scalaz.JsonScalaz._
import net.liftweb.json._


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
 *    .handleCode(HttpResponseCode.NotFound, (_: HttpResponse) => ...)
 *
 * }}}
 *
 * If the response is expected to be considered a success when it its code is `204 No Content`,
 * `expectNoContent(t: T)` can be called to return a `scalaz.Success` when the `IO` is performed.
 * See `expectNoContent` for more info.
 *
 */

trait ResponseHandlerDSL {
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

}