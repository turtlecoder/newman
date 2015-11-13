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

package com.stackmob.newman.test.client

import org.specs2.Specification
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import org.specs2.matcher._
import com.stackmob.newman.{Headers, Constants}
import java.nio.charset.Charset
import scalaz.Scalaz._
import org.json4s._
import org.json4s.scalaz.JsonScalaz._
import scala.util.Try

trait ResponseMatcher extends MustExpectations with MustMatchers { this: Specification with HeadersMatcher =>

  def beResponse(code: HttpResponseCode,
                 headers: Headers = None,
                 checkResponseBody: Boolean = true)
                (expectedResponseChecker: ExpectedResponseChecker)
                (implicit charset: Charset = Constants.UTF8Charset): ResponseMatcher = {
    new ResponseMatcher(code, headers, checkResponseBody)(expectedResponseChecker)(charset)
  }

  type ExpectedResponseChecker = ExpectedResponse => MatchResult[Any]

  class ResponseMatcher(expectedCode: HttpResponseCode,
                        headers: Headers = None,
                        checkResponseBody: Boolean = true)
                       (expectedResponseChecker: ExpectedResponse => MatchResult[Any])
                       (implicit charset: Charset = Constants.UTF8Charset) extends Matcher[HttpResponse] {

    def apply[S <: HttpResponse](expectable: Expectable[S]) = {
      val actualResp = expectable.value
      val descr = expectable.description

      val codeRes = actualResp.code must beEqualTo(expectedCode)

      val headerRes = actualResp.headers must containHeaders(headers)

      //keep this lazy because it isn't executed if checkResponseBody is false
      lazy val expectedResponseRes = {
        val tryJValue = Try {
          parse(actualResp.bodyString())
        }
        tryJValue must beSuccessfulTry.like {
          case json: JValue => {
            fromJSON[ExpectedResponse](json).toEither must beRight.like {
              case expectedResponse: ExpectedResponse => {
                expectedResponseChecker(expectedResponse)
              }
            }
          }
        }
      }

      val totalRes = if(checkResponseBody) {
        codeRes and headerRes and expectedResponseRes
      } else {
        codeRes and headerRes
      }

      result(totalRes,
        s"$descr matches code $expectedCode, contains headers ${headers.shows} and matches body",
        s"$descr does not match code $expectedCode, contain headers ${headers.shows} and match body",
        expectable)
    }
  }
}
