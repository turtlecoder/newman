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
import scalaz.effect.IO
import java.net.URL
import scalaz.Validation
import scalaz.concurrent.Promise
import net.liftweb.json.scalaz.JsonScalaz._
import java.nio.charset.Charset
import com.stackmob.newman.Constants._
import language.implicitConversions

package object dsl extends URLBuilderDSL with RequestBuilderDSL with ResponseHandlerDSL with AsyncResponseHandlerDSL {

  sealed trait Protocol {
    def name: String
  }

  case object http extends Protocol {
    override lazy val name = "http"
  }

  case object https extends Protocol {
    override lazy val name = "https"
  }

  implicit def urlCapableToURL(c: URLCapable): URL = c.toURL
  implicit def stringToPath(s: String): Path = Path(s :: Nil)

  implicit def transformerToHttpRequest(t: Builder): HttpRequest = t.toRequest

  implicit def responseHandlerToResponse[Failure, Success](handler: ResponseHandler[Failure, Success]): IOValidation[Failure, Success] = {
    handler.toIO
  }

  implicit def asyncResponseHandlerToResponse[Failure, Success](handler: AsyncResponseHandler[Failure, Success]): IOPromiseValidation[Failure, Success] = {
    handler.toIO
  }

  case class UnhandledResponseCode(code: HttpResponseCode, body: String)
    extends Exception("unhandled response code %d and body %s".format(code.code, body))

  private[this] def emptyHandlerList[Failure, Success] = {
    List[(HttpResponseCode => Boolean, HttpResponse => Validation[Failure, Success])]()
  }

  implicit class RichIOHttpResponse(value: IO[HttpResponse]) {

    def handleCodesSuchThat[Failure, Success](check: HttpResponseCode => Boolean)
                                             (handler: HttpResponse => Validation[Failure, Success])
                                             (implicit errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      ResponseHandler(emptyHandlerList[Failure, Success], value).handleCodesSuchThat(check)(handler)
    }

    def handleCode[Failure, Success](code: HttpResponseCode)
                                    (handler: HttpResponse => Validation[Failure, Success])
                                    (implicit errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      ResponseHandler(emptyHandlerList[Failure,Success], value).handleCode(code)(handler)
    }

    def handleCodes[Failure, Success](codes: Seq[HttpResponseCode])
                                     (handler: HttpResponse => Validation[Failure, Success])
                                     (implicit errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      ResponseHandler(emptyHandlerList[Failure,Success], value).handleCodes(codes)(handler)
    }

    def expectJSONBody[Failure, Success](code: HttpResponseCode)
                                        (implicit reader: JSONR[Success],
                                         m: Manifest[Success],
                                         charset: Charset = UTF8Charset,
                                         errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      ResponseHandler(emptyHandlerList[Failure,Success], value).expectJSONBody(code)(reader, m, charset)
    }

    def handleJSONBody[Failure, S, Success](code: HttpResponseCode)
                                           (handler: S => Validation[Failure, Success])
                                           (implicit reader: JSONR[S],
                                            m: Manifest[S],
                                            charset: Charset = UTF8Charset,
                                            errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      ResponseHandler(emptyHandlerList[Failure,Success], value).handleJSONBody(code)(handler)(reader, m, charset)
    }

    def expectNoContent[Failure, Success](successValue: Success)
                                         (implicit errorConv: Throwable => Failure): ResponseHandler[Failure, Success] = {
      ResponseHandler(emptyHandlerList[Failure,Success], value).expectNoContent(successValue)
    }
  }

  implicit class RichIOPromiseHttpResponse(value: IO[Promise[HttpResponse]]) {

    def handleCodesSuchThat[Failure, Success](check: HttpResponseCode => Boolean)
                                             (handler: HttpResponse => Validation[Failure, Success])
                                             (implicit errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      AsyncResponseHandler(emptyHandlerList[Failure, Success], value).handleCodesSuchThat(check)(handler)
    }

    def handleCode[Failure, Success](code: HttpResponseCode)
                                    (handler: HttpResponse => Validation[Failure, Success])
                                    (implicit errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      AsyncResponseHandler(emptyHandlerList[Failure,Success], value).handleCode(code)(handler)
    }

    def handleCodes[Failure, Success](codes: Seq[HttpResponseCode])
                                     (handler: HttpResponse => Validation[Failure, Success])
                                     (implicit errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      AsyncResponseHandler(emptyHandlerList[Failure,Success], value).handleCodes(codes)(handler)
    }

    def expectJSONBody[Failure, Success](code: HttpResponseCode)
                                        (implicit reader: JSONR[Success],
                                         m: Manifest[Success],
                                         charset: Charset = UTF8Charset,
                                         errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      AsyncResponseHandler(emptyHandlerList[Failure,Success], value).expectJSONBody(code)(reader, m, charset)
    }

    def handleJSONBody[Failure, S, Success](code: HttpResponseCode)
                                           (handler: S => Validation[Failure, Success])
                                           (implicit reader: JSONR[S],
                                            m: Manifest[S],
                                            charset: Charset = UTF8Charset,
                                            errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      AsyncResponseHandler(emptyHandlerList[Failure,Success], value).handleJSONBody(code)(handler)(reader, m, charset)
    }

    def expectNoContent[Failure, Success](successValue: Success)
                                         (implicit errorConv: Throwable => Failure): AsyncResponseHandler[Failure, Success] = {
      AsyncResponseHandler(emptyHandlerList[Failure,Success], value).expectNoContent(successValue)
    }
  }

}
