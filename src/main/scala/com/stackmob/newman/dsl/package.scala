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

package com.stackmob.newman

import request.HttpRequest
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import java.net.URL
import scalaz.Validation
import org.json4s.scalaz.JsonScalaz._
import java.nio.charset.Charset
import com.stackmob.newman.Constants._
import scala.concurrent.{ExecutionContext, Future}
import language.implicitConversions

package object dsl extends URLBuilderDSL with RequestBuilderDSL with ResponseHandlerDSL with AsyncResponseHandlerDSL {

  /**
   * the base from which protocols are derived
   */
  sealed trait Protocol {
    def name: String
  }

  /**
   * the HTTP protocol
   */
  case object http extends Protocol {
    override lazy val name = "http"
  }

  /**
   * the HTTPS protocol
   */
  case object https extends Protocol {
    override lazy val name = "https"
  }

  /**
   * converts a {{{URLCapable}}} object to a {{{URL}}}
   * @param c the object to convert
   * @return the converted {{{URL}}}
   */
  implicit def urlCapableToURL(c: URLCapable): URL = c.toURL

  /**
   * converts a {{{String}}} to a {{{Path}}}
   * @param s the string to convert
   * @return the converted {{{Path}}}
   */
  implicit def stringToPath(s: String): Path = {
    new Path(s :: Nil)
  }

  /**
   * converts a {{{Builder}}} to the {{{HttpRequest}}} it represents
   * @param t the {{{Builder}}} to convert
   * @return the resultant {{{HttpRequest}}}
   */
  implicit def transformerToHttpRequest(t: Builder): HttpRequest = {
    t.toRequest
  }

  /**
   * converts a {{{ResponseHandler[Failure, Success}}} to the {{{IO[Validation[Failure, Success]]}}} that results
   * from evaluating the rules outlined in the {{{ResponseHandler}}}
   * @param handler the response handler
   * @tparam Failure the failure type of the handler
   * @tparam Success the success type of the handler
   * @return the resultant {{{IO[Validation[Failure, Success]]}}}
   */
  implicit def responseHandlerToResponse[Failure, Success](handler: ResponseHandler[Failure, Success]): Validation[Failure, Success] = {
    handler.toValidation
  }

  /**
   * converts a {{{AsyncResponseHandler[Failure, Success]}}} to the {{{IO[Promise[Validation[Failure, Success]]]}}} that results
   * from evaluating the rules outlined in the {{{AsyncResponseHandler}}}
   * @param handler the response handler
   * @tparam Failure the failure type of the handler
   * @tparam Success the success type of the handler
   * @return the resultant {{{IO[Promise[Validation[Failure, Success]]]}}}
   */
  implicit def asyncResponseHandlerToResponse[Failure, Success](handler: AsyncResponseHandler[Failure, Success])
                                                               (implicit ctx: ExecutionContext): FutureValidation[Failure, Success] = {
    handler.toFutureValidation
  }

  /**
   * the exception generated when a response handler (ie {{{ResponseHandler}}}) encounters a response code for which
   * it doesn't have a rule to handle
   * @param code the response code that the response handler can't handle
   * @param body the body of the response that accompanied {{{code}}}
   */
  case class UnhandledResponseCode(code: HttpResponseCode, body: String)
    extends Exception("unhandled response code %d and body %s".format(code.code, body))

  private[this] def emptyHandlerList[Failure, Success] = {
    List[(HttpResponseCode => Boolean, HttpResponse => Validation[Failure, Success])]()
  }

  /**
   * a class extension for {{{IO[HttpResponse]}}} which provides the entry point into {{{ResponseHandler}}}.
   * in the following example usage, the {{{handleCode}}} call lives inside this extension
   * {{{val GET(new URL("http://localhost")).prepare.handleCode(....)}}}
   *
   * @param value the extended {{{IO}}}
   */
  implicit class RichIOHttpResponse(value: HttpResponse) {

    /**
     * see {{{ResponseHandler#handleCodesSuchThat}}}
     */
    def handleCodesSuchThat[Failure, Success](check: HttpResponseCode => Boolean)
                                             (handler: HttpResponse => Validation[Failure, Success])
                                             (implicit errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      new ResponseHandler(emptyHandlerList[Failure, Success], value).handleCodesSuchThat(check)(handler)
    }

    /**
     * see {{{ResponseHandler#handleCode}}}
     */
    def handleCode[Failure, Success](code: HttpResponseCode)
                                    (handler: HttpResponse => Validation[Failure, Success])
                                    (implicit errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      new ResponseHandler(emptyHandlerList[Failure,Success], value).handleCode(code)(handler)
    }

    /**
     * see {{{ResponseHandler#handleCodes}}}
     */
    def handleCodes[Failure, Success](codes: Seq[HttpResponseCode])
                                     (handler: HttpResponse => Validation[Failure, Success])
                                     (implicit errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      new ResponseHandler(emptyHandlerList[Failure,Success], value).handleCodes(codes)(handler)
    }

    /**
     * see {{{ResponseHandler#expectJSONBody}}}
     */
    def expectJSONBody[Failure, Success](code: HttpResponseCode)
                                        (implicit reader: JSONR[Success],
                                         m: Manifest[Success],
                                         charset: Charset = UTF8Charset,
                                         errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      new ResponseHandler(emptyHandlerList[Failure,Success], value).expectJSONBody(code)(reader, m, charset)
    }

    /**
     * see {{{ResponseHandler#handleJSONBody}}}
     */
    def handleJSONBody[Failure, S, Success](code: HttpResponseCode)
                                           (handler: S => Validation[Failure, Success])
                                           (implicit reader: JSONR[S],
                                            m: Manifest[S],
                                            charset: Charset = UTF8Charset,
                                            errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      new ResponseHandler(emptyHandlerList[Failure,Success], value).handleJSONBody(code)(handler)(reader, m, charset)
    }

    /**
     * see {{{ResponseHandler#expectNoContent}}}
     */
    def expectNoContent[Failure, Success](successValue: Success)
                                         (implicit errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      new ResponseHandler(emptyHandlerList[Failure,Success], value).expectNoContent(successValue)
    }
  }

  /**
   * a class extension for {{{IO[Promise[HttpResponse]]}}} which provides the entry point into {{{AsyncResponseHandler}}}.
   * in the following example usage, the {{{handleCode}}} call lives inside this extension
   * {{{val GET(new URL("http://localhost")).prepareAsync.handleCode(....)}}}
   *
   * @param value the extended {{{IO}}}
   */
  implicit class RichFutureHttpResponse(value: Future[HttpResponse]) {

    /**
     * see {{{AsyncResponseHandler#handleCodesSuchThat}}}
     */
    def handleCodesSuchThat[Failure, Success](check: HttpResponseCode => Boolean)
                                             (handler: HttpResponse => Validation[Failure, Success])
                                             (implicit errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      new AsyncResponseHandler(emptyHandlerList[Failure, Success], value).handleCodesSuchThat(check)(handler)
    }

    /**
     * see {{{AsyncResponseHandler#handleCode}}}
     */
    def handleCode[Failure, Success](code: HttpResponseCode)
                                    (handler: HttpResponse => Validation[Failure, Success])
                                    (implicit errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      new AsyncResponseHandler(emptyHandlerList[Failure,Success], value).handleCode(code)(handler)
    }

    /**
     * see {{{AsyncResponseHandler#handleCodes}}}
     */
    def handleCodes[Failure, Success](codes: Seq[HttpResponseCode])
                                     (handler: HttpResponse => Validation[Failure, Success])
                                     (implicit errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      new AsyncResponseHandler(emptyHandlerList[Failure,Success], value).handleCodes(codes)(handler)
    }

    /**
     * see {{{AsyncResponseHandler#expectJSONBody}}}
     */
    def expectJSONBody[Failure, Success](code: HttpResponseCode)
                                        (implicit reader: JSONR[Success],
                                         m: Manifest[Success],
                                         charset: Charset = UTF8Charset,
                                         errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      new AsyncResponseHandler(emptyHandlerList[Failure,Success], value).expectJSONBody(code)(reader, m, charset)
    }

    /**
     * see {{{AsyncResponseHandler#handleJSONBody}}}
     */
    def handleJSONBody[Failure, S, Success](code: HttpResponseCode)
                                           (handler: S => Validation[Failure, Success])
                                           (implicit reader: JSONR[S],
                                            m: Manifest[S],
                                            charset: Charset = UTF8Charset,
                                            errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      new AsyncResponseHandler(emptyHandlerList[Failure,Success], value).handleJSONBody(code)(handler)(reader, m, charset)
    }

    /**
     * see {{{AsyncResponseHandler#expectNoContent}}}
     */
    def expectNoContent[Failure, Success](successValue: Success)
                                         (implicit errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      new AsyncResponseHandler(emptyHandlerList[Failure,Success], value).expectNoContent(successValue)
    }
  }

}
