/**
 * Copyright 2012-2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.newman.dsl

import scalaz.Scalaz._
import scalaz.concurrent.Promise
import com.stackmob.newman.response.{HttpResponse, HttpResponseCode}
import scalaz.Validation
import scalaz.effect.IO
import net.liftweb.json.scalaz.JsonScalaz._
import java.nio.charset.Charset
import com.stackmob.newman.Constants._
import com.stackmob.newman._
import com.stackmob.newman.response.HttpResponse.JSONParsingError

/**
 * the same thing as {{{com.stackmob.newman.dsl.ResponseHandlerDSL}}}, except for asynchronous response handling
 */
trait AsyncResponseHandlerDSL {
  type IOPromise[T] = IO[Promise[T]]

  case class AsyncResponseHandler[Failure, Success](handlers: List[(HttpResponseCode => Boolean, HttpResponse => Validation[Failure, Success])],
                                                    respIO: IOPromise[HttpResponse])
                                                   (implicit errorConv: Throwable => Failure) {

    /**
     * Adds a handler (a function that is called when the code matches the given function) and returns a new ResponseHandler
     * @param check response code this handler is for
     * @param handler function to call when response with given code is encountered
     * @return
     */
    def handleCodesSuchThat(check: HttpResponseCode => Boolean)
                           (handler: HttpResponse => Validation[Failure, Success]): AsyncResponseHandler[Failure, Success] = {
      copy(handlers = (check, handler) :: handlers)
    }

    /**
     * Adds a handler (a function that is called when the given code is matched) and returns a new ResponseHandler
     * @param code response code this handler is for
     * @param handler function to call when response with given code is encountered
     * @return
     */
    def handleCode(code: HttpResponseCode)
                  (handler: HttpResponse => Validation[Failure, Success]): AsyncResponseHandler[Failure, Success] = {
      handleCodesSuchThat({ c: HttpResponseCode =>
        c === code
      })(handler)
    }

    /**
     * Adds a handler (a function that is called when any of the given codes are matched) and returns a new ResponseHandler
     * @param codes response code this handler matches
     * @param handler function to call when response with given code is encountered
     * @return
     */
    def handleCodes(codes: Seq[HttpResponseCode])
                   (handler: HttpResponse => Validation[Failure, Success]): AsyncResponseHandler[Failure, Success] = {
      handleCodesSuchThat(codes.contains(_))(handler)
    }

    /**
     * Adds a handler that expects the specified response code and a JSON body readable by
     * a JSONR for the type of this ResponseHandler. A Response can only return
     * one successful type per request so multiple calls to this method should not be
     * made, however, if there are there is no effect on the handling of the response.
     * @return a new [[com.stackmob.newman.dsl.ResponseHandler]]
     */
    def expectJSONBody(code: HttpResponseCode)
                      (implicit reader: JSONR[Success],
                       m: Manifest[Success],
                       charset: Charset = UTF8Charset): AsyncResponseHandler[Failure, Success] = {
      handleJSONBody[Success](code)(_.success[Failure])
    }

    /**
     * Adds a handler that expects the specified response code and a
     * JSON body readable by a JSONR for the type of this
     * ResponseHandler, which is then transformed by the supplied
     * function. A Response can only return one successful type per
     * request so multiple calls to this method should not be made,
     * however, if there are there is no effect on the handling of the
     * response.  @return a new [[com.stackmob.newman.dsl.ResponseHandler]]
     */
    def handleJSONBody[S](code: HttpResponseCode)
                         (handler: S => Validation[Failure, Success])
                         (implicit reader: JSONR[S],
                          m: Manifest[S],
                          charset: Charset = UTF8Charset): AsyncResponseHandler[Failure, Success] = {
      handleCode(code)((resp: HttpResponse) => resp.bodyAs[S].leftMap { t =>
        errorConv(JSONParsingError(t): Throwable): Failure
      }.flatMap(handler))
    }

    /**
     * Adds a handler that expects a 204 response. If encountered, the value passed to this method
     * will be returned when the handler is sealed and the IO is performed. This method should
     * only be called once per response but multiple calls have no effect
     * @param successValue - the value to return successfully when a 204 is encountered
     * @return a new ResponseHandler
     */
    def expectNoContent(successValue: Success): AsyncResponseHandler[Failure, Success] = {
      handleCode(HttpResponseCode.NoContent)((_: HttpResponse) => successValue.success)
    }

    /**
     * Provide a default handler for all unhandled status codes. Must be the last handler in the chain
     */
    def default(handler: HttpResponse => Validation[Failure, Success]): IOPromiseValidation[Failure, Success] = {
      respIO.map { responseProm: Promise[HttpResponse] =>
        responseProm.map { response =>
          handlers.reverse.find { functionTup =>
            functionTup._1.apply(response.code)
          }.map { functionTup =>
            functionTup._2.apply(response)
          } | {
            handler(response)
          }
        }
      }.except { t =>
        errorConv(t).fail[Success].pure[Promise].pure[IO]
      }
    }

    def toIO: IOPromiseValidation[Failure, Success] = {
      default { resp =>
        errorConv.apply(UnhandledResponseCode(resp.code, resp.bodyString)).fail[Success]
      }
    }
  }
}
