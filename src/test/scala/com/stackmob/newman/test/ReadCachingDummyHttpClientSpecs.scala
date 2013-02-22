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

import caching.DummyHttpResponseCacher
import scalacheck._

import org.specs2.{ScalaCheck, Specification}
import com.stackmob.newman.caching.HttpResponseCacher
import com.stackmob.newman.{HttpClient, ReadCachingHttpClient}
import com.stackmob.newman.response.HttpResponse
import org.scalacheck._
import Prop._

class ReadCachingDummyHttpClientSpecs extends Specification with ScalaCheck { def is =
  "ReadCachingDummyHttpClientSpecs".title                                                                               ^ end ^
  "CachingDummyHttpClient is an HttpClient that caches responses for some defined TTL"                                  ^ end ^
  "get should read from the cache if there is an entry already"                                                         ! getReadsOnlyFromCache ^ end ^
  "get should read from the client if no cache entry, and add to the cache"                                             ! skipped ^ end ^
  "head should read from the cache if there is a cache entry already"                                                   ! skipped ^ end ^
  "head should read from the client if no cache entry, and add to the cache"                                            ! skipped ^ end ^
  "POST, PUT, DELETE should not touch the cache"                                                                        ! skipped ^ end

  private val genNoHttpResponse: Gen[Option[HttpResponse]] = Gen.value(Option.empty[HttpResponse])
  private val genAlwaysHttpResponse: Gen[Option[HttpResponse]] = for {
    resp <- genHttpResponse
  } yield {
    Some(resp)
  }

  private def genDummyHttpResponseCache(genMbResp: Gen[Option[HttpResponse]]): Gen[DummyHttpResponseCacher] = for {
    mbResp <- genMbResp
    exists <- Gen.oneOf(true, false)
  } yield {
    new DummyHttpResponseCacher(mbResp, (), exists)
  }

  private def genDummyHttpClient: Gen[DummyHttpClient] = for {
    resp <- genHttpResponse
  } yield {
    new DummyHttpClient(() => resp)
  }

  private def getReadsOnlyFromCache = forAll(genURL,
    genHeaders,
    genDummyHttpClient,
    genDummyHttpResponseCache(genAlwaysHttpResponse),
    genPositiveMilliseconds) { (url, headers, dummyClient, dummyCache, milliseconds) =>
    val client = new ReadCachingHttpClient(dummyClient, dummyCache, milliseconds)

    val respMatches = dummyCache.cannedGet must beSome.like {
      case r => r must beEqualTo(client.get(url, headers).executeUnsafe)
    }
    val oneCacheGet = dummyCache.getCalls.size must beEqualTo(1)
    val noCacheSets = dummyCache.setCalls.size must beEqualTo(0)
    val noCacheExists = dummyCache.existsCalls.size must beEqualTo(0)
    val noClientGets = dummyClient.getRequests.size must beEqualTo(0)
    val noClientPosts = dummyClient.postRequests.size must beEqualTo(0)
    val noClientPuts = dummyClient.putRequests.size must beEqualTo(0)
    val noClientDeletes = dummyClient.deleteRequests.size must beEqualTo(0)
    val noClientHeads = dummyClient.headRequests.size must beEqualTo(0)

    respMatches and
    oneCacheGet and
    noCacheSets and
    noCacheExists and
    noClientGets and
    noClientPosts and
    noClientPuts and
    noClientDeletes and
    noClientHeads
  }
}
