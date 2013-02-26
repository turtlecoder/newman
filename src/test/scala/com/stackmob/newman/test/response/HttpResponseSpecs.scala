/**
 * Copyright 2013 StackMob
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
import org.specs2.execute.{Result => SpecsResult, Failure => SpecsFailure}
import com.stackmob.newman.test.BaseContext
import com.stackmob.newman._
import com.stackmob.newman.response._
import org.specs2.matcher.{MatchFailure, MatchResult}

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
    def correctlyFails: SpecsResult = resp.bodyAsIfResponseCode[String](expectedRespCode, { resp: HttpResponse =>
      (new Exception("shouldn't be thrown")).fail
    }).map { _: String =>
      SpecsFailure("bodyAsIfResponseCode succeeded when it should have failed")
    } ||| { t: Throwable =>
      (t must beAnInstanceOf[HttpResponse.UnexpectedResponseCode]) and
      (t.cast[HttpResponse.UnexpectedResponseCode].map { ex: HttpResponse.UnexpectedResponseCode =>
        {
          (ex.expected must beEqualTo(expectedRespCode)) and
          (ex.actual must beEqualTo(actualRespCode))
        }: MatchResult[_]
      } | {
        MatchFailure("ok", "returned exception was not an HttpResponse.UnexpectedResponseCode", (false must beTrue).expectable)
      })
    }
  }

  case class FailureDecodingBody() extends Context {
    protected val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    protected val decodingEx = new Exception("test decoding exception")
    def correctlyFails: SpecsResult = resp.bodyAsIfResponseCode[String](HttpResponseCode.Ok, { r: HttpResponse =>
      decodingEx.fail
    }).map { s: String =>
      SpecsFailure("bodyAsIfResponseCode succeeded when it should have failed")
    } ||| { t: Throwable =>
      t must beEqualTo(decodingEx)
    }
  }

  case class SuccessDecodingBody() extends Context {
    protected val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    protected val decodedBody = "testResp"
    def succeeds: SpecsResult = resp.bodyAsIfResponseCode(HttpResponseCode.Ok, r => decodedBody.success).map { s: String =>
      (s must beEqualTo(decodedBody)): SpecsResult
    } ||| (logAndFail(_))
  }

  case class ThrowsDecodingBody() extends Context {
    private val ex = new Exception("test exception")
    private val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    def fails: SpecsResult = resp.bodyAsIfResponseCode(resp.code, (_ => throw ex)).map { n: Nothing =>
      SpecsFailure("method succeeded when it should not have")
    } ||| { t: Throwable =>
      t must beEqualTo(ex)
    }
  }
}