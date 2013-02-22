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
package caching

import scalaz._
import Scalaz._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import com.stackmob.newman._
import com.stackmob.newman.caching._
import com.stackmob.newman.response._
import java.net.URL

class InMemoryHttpResponseCacherSpecs extends Specification { def is =
  "InMemoryHttpResponseCacherSpecs".title                                                                               ^
  "The InMemoryHttpResponseCacher implements an HttpResponseCacher in memory, in a thread-safe manner"                  ^
  "The cacher should"                                                                                                   ^
    "correctly round trip an HttpRequest"                                                                               ! RoundTrip().succeeds ^
                                                                                                                        end
  trait Context extends BaseContext {
    protected val client = new DummyHttpClient
    protected val request = client.get(new URL("http://stackmob.com"), Headers.empty)
    protected val responseFn = client.responseToReturn
    protected val cache: HttpResponseCacher = new InMemoryHttpResponseCacher
  }

  case class RoundTrip() extends Context {
    def succeeds: SpecsResult = {
      (cache.get(request).unsafePerformIO must beEqualTo(Option.empty[HttpResponse])) and
      (cache.exists(request).unsafePerformIO must beFalse) and
      (cache.set(request, responseFn(), Time.now).unsafePerformIO must beEqualTo(())) and
      (cache.get(request).unsafePerformIO must beEqualTo(responseFn().some)) and
      (cache.exists(request).unsafePerformIO must beTrue)
    }
  }
}
