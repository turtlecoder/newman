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
import com.stackmob.newman.caching.{Milliseconds, HttpResponseCacher}
import com.stackmob.newman.response.HttpResponse
import org.scalacheck._
import Prop._
import java.net.URL
import com.stackmob.newman._
import com.stackmob.newman.request.HttpRequestWithoutBody

class ReadCachingDummyHttpClientSpecs
  extends Specification
  with ScalaCheck
  with ClientVerification
  with CacheVerification { def is =
  "ReadCachingDummyHttpClientSpecs".title                                                                               ^ end ^
  "CachingDummyHttpClient is an HttpClient that caches responses for some defined TTL"                                  ^ end ^
  "get should read from the cache if there is an entry already"                                                         ! getReadsOnlyFromCache ^ end ^
  "get should read through to the cache"                                                                                ! getReadsThroughToCache ^ end ^
  "head should read from the cache if there is a cache entry already"                                                   ! headReadsOnlyFromCache ^ end ^
  "head should read through to the cache"                                                                               ! headReadsThroughToCache ^ end ^
  "POST, PUT, DELETE should not touch the cache"                                                                        ! postPutDeleteIgnoreCache ^ end

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

  private def createClient(underlying: HttpClient, cache: HttpResponseCacher, millis: Milliseconds) = {
    new ReadCachingHttpClient(underlying, cache, millis)
  }

  private def verifyReadsOnlyFromCache[T <: HttpRequestWithoutBody](fn: (ReadCachingHttpClient, URL, Headers) => T) = {
    forAll(genURL,
      genHeaders,
      genDummyHttpClient,
      genDummyHttpResponseCache(genAlwaysHttpResponse),
      genPositiveMilliseconds) { (url, headers, dummyClient, dummyCache, milliseconds) =>
      val client = createClient(dummyClient, dummyCache, milliseconds)

      val req = fn(client, url, headers)
      val resp = req.executeUnsafe
      val respMatches = dummyCache.cannedGet must beSome.like {
        case r => {
          r must beEqualTo(resp)
        }
      }

      respMatches and
      verifyCacheInteraction(dummyCache, CacheInteraction(1, 0, 0))
      verifyClientInteraction(dummyClient, ClientInteraction(0, 0, 0, 0, 0))

    }
  }

  private def verifyReadsThroughToCache[T <: HttpRequestWithoutBody](createRequest: (ReadCachingHttpClient, URL, Headers) => T,
                                                                     createClientInteraction: Int => ClientInteraction) = {
    forAll(genURL,
      genHeaders,
      genDummyHttpClient,
      genDummyHttpResponseCache(genNoHttpResponse)) { (url, headers, dummyClient, dummyCache) =>

      val client = createClient(dummyClient, dummyCache, Milliseconds(60000))
      val req = createRequest(client, url, headers)

      val expectedResponse = dummyClient.responseToReturn()

      val resp1 = req.executeUnsafe
      val resp1Verified = resp1 must beEqualTo(expectedResponse)
      val resp1ClientVerified = verifyClientInteraction(dummyClient, createClientInteraction(1))
      //there should be a get call, a miss, then a set call to perform the write back to the cache
      val resp1CacheVerified = verifyCacheInteraction(dummyCache, CacheInteraction(1, 1, 0))

      val resp2 = req.executeUnsafe
      val resp2Verified = resp2 must beEqualTo(expectedResponse)
      //it shouldn't hit the backing client (ie the dummy client) again
      val resp2ClientVerified = verifyClientInteraction(dummyClient, createClientInteraction(1))
      //there should be 2 get calls now, and still only 1 read back to the cache
      val resp2CacheVerified = verifyCacheInteraction(dummyCache, CacheInteraction(2, 1, 0))

      resp1Verified and
      resp1ClientVerified and
      resp1CacheVerified and
      resp2Verified and
      resp2ClientVerified and
      resp2CacheVerified
    }
  }

  private def getReadsOnlyFromCache = verifyReadsOnlyFromCache { (client, url, headers) =>
    client.get(url, headers)
  }
  private def getReadsThroughToCache = verifyReadsThroughToCache( { (client, url, headers) =>
    client.get(url, headers)
  }, { numGets =>
    ClientInteraction(numGets, 0, 0, 0, 0)
  })

  private def headReadsOnlyFromCache = verifyReadsOnlyFromCache { (client, url, headers) =>
    client.head(url, headers)
  }
  private def headReadsThroughToCache = verifyReadsThroughToCache({ (client, url, headers) =>
    client.head(url, headers)
  }, { numHeads =>
    ClientInteraction(0, 0, 0, 0, numHeads)
  })

  private def postPutDeleteIgnoreCache = forAll(genURL,
    genHeaders,
    genRawBody,
    genDummyHttpClient,
    genDummyHttpResponseCache(genAlwaysHttpResponse),
    genPositiveMilliseconds) { (url, headers, body, dummyClient, dummyCache, milliseconds) =>
    val client = new ReadCachingHttpClient(dummyClient, dummyCache, milliseconds)

    val postRes = client.post(url, headers, body).executeUnsafe must beEqualTo(dummyClient.responseToReturn())
    val putRes = client.put(url, headers, body).executeUnsafe must beEqualTo(dummyClient.responseToReturn())
    val deleteRes = client.delete(url, headers).executeUnsafe must beEqualTo(dummyClient.responseToReturn())

    postRes and
    putRes and
    deleteRes and
    verifyCacheInteraction(dummyCache, CacheInteraction(0, 0, 0)) and
    verifyClientInteraction(dummyClient, ClientInteraction(0, 1, 1, 1, 0))
  }
}

trait CacheVerification { this: Specification =>
  protected case class CacheInteraction(numGets: Int, numSets: Int, numExists: Int)
  protected def verifyCacheInteraction(cache: DummyHttpResponseCacher, interaction: CacheInteraction) = {
    (cache.getCalls.size must beEqualTo(interaction.numGets)) and
    (cache.setCalls.size must beEqualTo(interaction.numSets)) and
    (cache.existsCalls.size must beEqualTo(interaction.numExists))
  }
}

trait ClientVerification { this: Specification =>
  protected case class ClientInteraction(numGets: Int, numPosts: Int, numPuts: Int, numDeletes: Int, numHeads: Int)
  protected def verifyClientInteraction(client: DummyHttpClient, interaction: ClientInteraction) = {
    (client.getRequests.size must beEqualTo(interaction.numGets)) and
    (client.postRequests.size must beEqualTo(interaction.numPosts)) and
    (client.putRequests.size must beEqualTo(interaction.numPuts)) and
    (client.deleteRequests.size must beEqualTo(interaction.numDeletes)) and
    (client.headRequests.size must beEqualTo(interaction.numHeads))
  }

}
