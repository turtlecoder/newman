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
import java.net.URL
import com.stackmob.newman.response.{HttpResponse, HttpResponseCode}
import com.stackmob.newman._
import com.stackmob.newman.dsl._
import org.specs2.matcher.MatchResult

class ApacheHttpClientSpecs extends Specification { def is =
  "ApacheHttpClientSpecs".title                                                                                         ^ end ^
  """
  ApacheHttpClient is the HttpClient implementation that actually hits the internet
  """                                                                                                                   ^ end ^
  "The Client Should"                                                                                                   ^
    "Correctly do GET requests"                                                                                         ! Get().succeeds ^
    "Correctly do async GET requests"                                                                                   ! Get().succeedsAsync ^
    "Correctly do POST requests"                                                                                        ! Post().succeeds ^
    "Correctly do async POST requests"                                                                                  ! Post().succeedsAsync ^
    "Correctly do PUT requests"                                                                                         ! Put().succeeds ^
    "Correctly do async PUT requests"                                                                                   ! Put().succeedsAsync ^
    "Correctly do DELETE requests"                                                                                      ! Delete().succeeds ^
    "Correctly do async DELETE requests"                                                                                ! Delete().succeedsAsync ^
    "Correctly do HEAD requests"                                                                                        ! Head().succeeds ^
    "Correctly do async HEAD requests"                                                                                  ! Head().succeedsAsync ^
                                                                                                                        end
  trait Context extends BaseContext {
    implicit protected val httpClient = new ApacheHttpClient

    protected def execute[T](t: Builder,
                             expectedCode: HttpResponseCode = HttpResponseCode.Ok)
                            (fn: HttpResponse => MatchResult[T]) = {
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

    protected lazy val getURL = new URL("http://httpbin.org/get")
    protected lazy val postURL = new URL("http://httpbin.org/post")
    protected lazy val putURL = new URL("http://httpbin.org/put")
    protected lazy val deleteURL = new URL("http://httpbin.org/delete")
    protected lazy val headURL = new URL("http://httpbin.org/get")

    implicit private val encoding = Constants.UTF8Charset

    protected def ensureHttpOk(h: HttpResponse) = h.code must beEqualTo(HttpResponseCode.Ok)
  }

  case class Get() extends Context {
    private val get = GET(getURL)
    def succeeds = execute(get)(ensureHttpOk(_))
    def succeedsAsync = executeAsync(get)(ensureHttpOk(_))
  }

  case class Post() extends Context {
    private val post = POST(postURL)
    def succeeds = execute(post)(ensureHttpOk(_))
    def succeedsAsync = executeAsync(post)(ensureHttpOk(_))
  }

  case class Put() extends Context {
    private val put = PUT(putURL)
    def succeeds = execute(put)(ensureHttpOk(_))
    def succeedsAsync = executeAsync(put)(ensureHttpOk(_))
  }

  case class Delete() extends Context {
    private val delete = DELETE(deleteURL)
    def succeeds = execute(delete)(ensureHttpOk(_))
    def succeedsAsync = executeAsync(delete)(ensureHttpOk(_))
  }

  case class Head() extends Context {
    private val head = HEAD(headURL)
    def succeeds = execute(head)(ensureHttpOk(_))
    def succeedsAsync = executeAsync(head)(ensureHttpOk(_))
  }

}
