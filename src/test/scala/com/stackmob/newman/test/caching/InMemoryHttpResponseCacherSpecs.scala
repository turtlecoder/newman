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
package test
package caching

import com.stackmob.newman.caching._
import com.stackmob.newman.test.scalacheck._
import org.specs2.{ScalaCheck, Specification}
import org.scalacheck._
import Prop._
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import com.stackmob.newman.concurrent.SequentialExecutionContext

class InMemoryHttpResponseCacherSpecs extends Specification with ScalaCheck { def is =
  "InMemoryHttpResponseCacherSpecs".title                                                                               ^ end ^
  "The InMemoryHttpResponseCacher implements an HttpResponseCacher in memory, in a thread-safe manner"                  ^ end ^
  "The cacher should correctly round trip an HttpRequest"                                                               ! roundTripSucceeds ^ end ^
  "The cacher should correctly expire items after their TTL"                                                            ! ttlSucceeds ^ end ^
                                                                                                                        end
  private val client = {
    new DummyHttpClient()
  }

  private def roundTripSucceeds = forAll(genHttpRequest(client)) { request =>
    val cache = new InMemoryHttpResponseCacher(maxCapacity = Int.MaxValue,
      initialCapacity = 1,
      timeToIdle = Duration(1, TimeUnit.SECONDS),
      timeToLive = Duration(2, TimeUnit.SECONDS))

    val response = request.block()
    val getRes1 = cache.get(request) must beNone
    val existsRes1 = cache.exists(request) must beFalse
    val setRes1 = cache.set(request, Future.successful(response)).block() must beEqualTo(response)
    val identicalRequest = client.get(request.url, request.headers)
    val getRes2 = cache.get(identicalRequest) must beSome.like {
      case s => s.block() must beEqualTo(response)
    }
    val existsRes2 = cache.exists(request) must beTrue
    getRes1 and existsRes1 and setRes1 and getRes2 and existsRes2
  }

  private def ttlSucceeds = forAll(genHttpRequest(client)) { request =>
    val cache = new InMemoryHttpResponseCacher(initialCapacity = 1,
      maxCapacity = Int.MaxValue,
      timeToIdle = Duration(10, TimeUnit.MILLISECONDS),
      timeToLive = Duration(20, TimeUnit.MILLISECONDS))

    cache.set(request, request.apply)
    Thread.sleep(100)
    val identicalRequest = client.get(request.url, request.headers)
    cache.get(identicalRequest) must beNone
  }
}
