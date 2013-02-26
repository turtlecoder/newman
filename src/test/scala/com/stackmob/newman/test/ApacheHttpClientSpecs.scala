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

package com.stackmob.newman.test

import org.specs2.Specification
import java.net.URL
import com.stackmob.newman.response.{HttpResponse, HttpResponseCode}
import com.stackmob.newman._
import com.stackmob.newman.dsl._
import org.specs2.matcher.MatchResult

class ApacheHttpClientSpecs extends Specification { def is =
  "ApacheHttpClientSpecs".title                                                                                         ^
  """
  ApacheHttpClient is the HttpClient implementation that actually hits the internet
  """                                                                                                                   ^
  "The Client Should"                                                                                                   ^
    "Correctly do GET requests"                                                                                         ! Get().succeeds ^
    "Correctly do async GET requests"                                                                                   ! Get().succeedsAsync ^
    "Correctly do POST requests"                                                                                        ! skipped ^ //Post().succeeds ^
    "Correctly do async POST requests"                                                                                  ! skipped ^ //Post().succeedsAsync ^
    "Correctly do PUT requests"                                                                                         ! skipped ^ //Put().succeeds ^
    "Correctly do async PUT requests"                                                                                   ! skipped ^ //Put().succeedsAsync ^
    "Correctly do DELETE requests"                                                                                      ! skipped ^ //Delete().succeeds ^
    "Correctly do async DELETE requests"                                                                                ! skipped ^ //Delete().succeedsAsync ^
    "Correctly do HEAD requests"                                                                                        ! Head().succeeds ^
    "Correctly do async HEAD requests"                                                                                  ! Head().succeedsAsync ^
                                                                                                                        end
  trait Context extends BaseContext {
    implicit protected val httpClient = new ApacheHttpClient

    protected def execute(t: Builder,
                          expectedCode: HttpResponseCode = HttpResponseCode.Ok)
                         (fn: HttpResponse => MatchResult[_]) = {
      val r = t.executeUnsafe
      r.code must beEqualTo(expectedCode) and fn(r)
    }

    protected def executeAsync(t: Builder,
                               expectedCode: HttpResponseCode = HttpResponseCode.Ok)
                              (fn: HttpResponse => MatchResult[_]) = {
      val rPromise = t.executeAsyncUnsafe
      rPromise.map { r: HttpResponse =>
        r.code must beEqualTo(expectedCode) and fn(r)
      }.get
    }

    protected lazy val url = new URL("https://www.stackmob.com")

    implicit private val encoding = Constants.UTF8Charset
    protected def ensureHttpOK(h: HttpResponse) = h.code must beEqualTo(HttpResponseCode.Ok)
    protected def ensureHtmlReturned(h: HttpResponse) = {
      (h.bodyString() must contain("html")) and
      (h.bodyString() must contain("/html"))
    }

    protected def ensureHtmlResponse(h: HttpResponse) = ensureHttpOK(h) and ensureHtmlReturned(h)

  }

  case class Get() extends Context {
    def succeeds = execute(GET(url))(ensureHtmlResponse(_))
    def succeedsAsync = executeAsync(GET(url))(ensureHtmlResponse(_))
  }

  case class Post() extends Context {
    private val post = POST(url)
    def succeeds = execute(post)(ensureHtmlResponse(_))
    def succeedsAsync = executeAsync(post)(ensureHtmlResponse(_))
  }

  case class Put() extends Context {
    private val put = PUT(url)
    def succeeds = execute(put)(ensureHtmlResponse(_))
    def succeedsAsync = executeAsync(put)(ensureHtmlResponse(_))
  }

  case class Delete() extends Context {
    private val delete = DELETE(url)
    def succeeds = execute(delete)(ensureHtmlResponse(_))
    def succeedsAsync = executeAsync(delete)(ensureHtmlResponse(_))
  }

  case class Head() extends Context {
    private val head = HEAD(url)
    def succeeds = execute(head)(ensureHttpOK(_))
    def succeedsAsync = executeAsync(head)(ensureHttpOK(_))
  }

}
