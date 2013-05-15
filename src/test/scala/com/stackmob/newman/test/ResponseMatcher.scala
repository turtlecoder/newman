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

package com.stackmob.newman.test

import org.specs2.Specification
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import org.specs2.matcher.{Expectable, Matcher}
import com.stackmob.newman.{Headers, HeaderList, Constants}
import com.stackmob.newman.Headers.HeadersShow
import java.nio.charset.Charset
import scalaz.Scalaz._

trait ResponseMatcher { this: Specification =>
  class ResponseMatcher(expectedCode: HttpResponseCode,
                        headers: Headers = None,
                        mbBodyPieces: Option[List[String]] = None)
                       (implicit charset: Charset = Constants.UTF8Charset) extends Matcher[HttpResponse] {
    def apply[S <: HttpResponse](expectable: Expectable[S]) = {
      val actualResp = expectable.value
      val descr = expectable.description
      val codeRes = actualResp.code must beEqualTo(expectedCode)

      val mbHeaderLists = actualResp.headers tuple headers
      val headerRes = mbHeaderLists must beSome.like {
        case tup: (HeaderList, HeaderList) => {
          val (actualHeaderList, expectedHeaderList) = tup
          actualHeaderList.list must contain(expectedHeaderList.list)
        }
      } or {
        mbHeaderLists must beNone
      }

      val bodyRes = mbBodyPieces must beSome.like {
        case pieces: List[String] => {
          pieces must haveAllElementsLike {
            case piece => {
              actualResp.bodyString must contain(piece)
            }
          }
        }
      } or {
        mbBodyPieces must beNone
      }

      val showHeaders = headers.shows

      val showBody = mbBodyPieces.map { pieces =>
        pieces.mkString(", ")
      }.getOrElse("(nothing)")

      result(codeRes and headerRes and bodyRes,
        s"$descr matches code $expectedCode, contains headers $showHeaders and contains body pieces $showBody",
        s"$descr does not match code $expectedCode, contain headers $showHeaders and contain body pieces $showBody",
        expectable)
    }
  }

  def beResponse(c: HttpResponseCode,
                 headers: Headers = None,
                 mbBodyPieces: Option[List[String]] = None)
                (implicit charset: Charset = Constants.UTF8Charset) = {
    new ResponseMatcher(c, headers, mbBodyPieces)
  }
}
