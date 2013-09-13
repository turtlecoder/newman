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

package com.stackmob.newman.test.response

import scalaz._
import Scalaz._
import org.specs2.Specification
import com.stackmob.newman.test.BaseContext
import com.stackmob.newman._
import com.stackmob.newman.response._

class HttpResponseSpecs extends Specification { def is =
  "HttpResponseSpecs".title                                                                                             ^
  """
  HttpResponse is the standard representation in Newman of an Http response
  """                                                                                                                   ^
  "bodyAsIfResponseCode should"                                                                                         ^
    "correctly return an UnexpectedResponseCode if the returned code was not expected"                                  ! UnexpectedResponseCode().correctlyFails ^
    "correctly return the exception returned from the decoder"                                                          ! FailureDecodingBody().correctlyFails ^
    "correectly return the value returned from the decoder"                                                             ! SuccessDecodingBody().succeeds ^
    "correctly return a failure if the decoder threw"                                                                   ! ThrowsDecodingBody().fails ^
                                                                                                                        end

  trait Context extends BaseContext

  case class UnexpectedResponseCode() extends Context {
    protected val expectedRespCode = HttpResponseCode.Ok
    protected val actualRespCode = HttpResponseCode.Unauthorized
    protected val resp = HttpResponse(actualRespCode, Headers.empty, RawBody.empty)
    def correctlyFails = {
      resp.bodyAsIfResponseCode[String](expectedRespCode, { resp: HttpResponse =>
        val ex = new Exception("shouldn't be thrown")
        ex.fail
      }).toEither must beLeft.like {
        case t: HttpResponse.UnexpectedResponseCode => {
          (t.expected must beEqualTo(expectedRespCode)) and
          (t.actual must beEqualTo(actualRespCode))
        }
      }
    }
  }

  case class FailureDecodingBody() extends Context {
    protected val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    protected val decodingEx = new Exception("test decoding exception")
    def correctlyFails = {
      resp.bodyAsIfResponseCode[String](HttpResponseCode.Ok, { r: HttpResponse =>
        decodingEx.fail
      }).toEither must beLeft.like {
        case t => t must beEqualTo(decodingEx)
      }
    }
  }

  case class SuccessDecodingBody() extends Context {
    protected val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    protected val decodedBody = "testResp"
    def succeeds = {
      resp.bodyAsIfResponseCode(HttpResponseCode.Ok, r => decodedBody.success).toEither must beRight.like {
        case s => s must beEqualTo(decodedBody)
      }
    }
  }

  case class ThrowsDecodingBody() extends Context {
    private val ex = new Exception("test exception")
    private val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    private def decoder[T](res: HttpResponse): ThrowableValidation[T] = {
      throw ex
      ex.fail[T]
    }
    def fails = {
      resp.bodyAsIfResponseCode(resp.code, decoder).toEither must beLeft.like {
        case t => t must beEqualTo(ex)
      }
    }
  }
}