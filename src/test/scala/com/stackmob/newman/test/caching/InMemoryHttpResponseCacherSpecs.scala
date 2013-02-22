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

import com.stackmob.newman._
import com.stackmob.newman.caching._
import com.stackmob.newman.test.scalacheck._
import org.specs2.{ScalaCheck, Specification}
import java.net.URL
import java.util.concurrent.TimeUnit
import org.scalacheck._
import Prop._
import request._

class InMemoryHttpResponseCacherSpecs extends Specification with ScalaCheck { def is =
  "InMemoryHttpResponseCacherSpecs".title                                                                               ^ end ^
  "The InMemoryHttpResponseCacher implements an HttpResponseCacher in memory, in a thread-safe manner"                  ^ end ^
  "The cacher should correctly round trip an HttpRequest"                                                               ! roundTripSucceeds ^ end ^
  "The cacher should correctly expire items after their TTL"                                                            ! ttlSucceeds ^ end ^
                                                                                                                        end
  private val client = new DummyHttpClient()

  private val genHeader: Gen[Header] = for {
    key <- genNonEmptyString
    value <- genNonEmptyString
  } yield {
    key -> value
  }

  private val genHeaders: Gen[Headers] = for {
    headers <- Gen.listOf(genHeader)
  } yield {
    Headers(headers)
  }

  private val genRequest: Gen[HttpRequest] = for {
    urlString <- genNonEmptyString
    url <- Gen.value(new URL("http://%s.com".format(urlString)))
    headers <- genHeaders
  } yield {
    client.get(url, headers)
  }

  private val genCache: Gen[HttpResponseCacher] = {
    Gen.value(new InMemoryHttpResponseCacher)
  }

  private def getResponse(request: HttpRequest) = {
    request.prepare.unsafePerformIO
  }

  private def roundTripSucceeds = forAll(genRequest, genCache) { (request, cache) =>
    val response = getResponse(request)
    val getRes1 = cache.get(request).unsafePerformIO must beNone
    val existsRes1 = cache.exists(request).unsafePerformIO must beFalse
    val setRes1 = cache.set(request, response, Milliseconds(1000)).unsafePerformIO must beEqualTo(())
    val getRes2 = cache.get(request).unsafePerformIO must beSome.like {
      case s => s must beEqualTo(response)
    }
    val existsRes2 = cache.exists(request).unsafePerformIO must beTrue
    getRes1 and existsRes1 and setRes1 and getRes2 and existsRes2
  }

  private def ttlSucceeds = forAll(genRequest, genCache) { (request, cache) =>
    val response = getResponse(request)
    cache.set(request, response, Milliseconds(0)).unsafePerformIO
    Thread.sleep(100)
    cache.get(request).unsafePerformIO must beNone
  }
}
