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
import com.stackmob.newman.{Headers, RawBody}

trait ResponseMatcher { this: Specification =>
  class ResponseMatcher(expectedCode: HttpResponseCode,
                        mbHeaders: Option[Headers] = None,
                        mbBody: Option[RawBody] = None) extends Matcher[HttpResponse] {
    def apply[S <: HttpResponse](expectable: Expectable[S]) = {
      val actualResp = expectable.value
      val descr = expectable.description
      val codeRes = actualResp.code must beEqualTo(expectedCode)
      result(codeRes,
        s"$descr matches code $expectedCode",
        s"$descr does not match code $expectedCode",
        expectable)
    }
  }

  def beResponse(c: HttpResponseCode,
                 mbHeaders: Option[Headers] = None,
                 mbBody: Option[RawBody] = None) = {
    new ResponseMatcher(c, mbHeaders, mbBody)
  }
}
