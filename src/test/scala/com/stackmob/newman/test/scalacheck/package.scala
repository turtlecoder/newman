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

import org.scalacheck.Gen
import java.util.concurrent.TimeUnit
import com.stackmob.newman._
import com.stackmob.newman.caching._
import com.stackmob.newman.request._
import java.net.URL
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import HttpResponseCode._

package object scalacheck {
  val genNonEmptyString: Gen[String] = for {
    firstChar <- Gen.listOf1(Gen.alphaChar)
    remainingChars <- Gen.listOf(Gen.alphaChar)
  } yield {
    val charList = List(firstChar) ++ remainingChars
    charList.mkString
  }

  private val timeUnits = Seq[TimeUnit](TimeUnit.DAYS,
    TimeUnit.HOURS,
    TimeUnit.MICROSECONDS,
    TimeUnit.MILLISECONDS,
    TimeUnit.MINUTES,
    TimeUnit.NANOSECONDS,
    TimeUnit.SECONDS)

  val genTimeUnit: Gen[TimeUnit] = Gen.oneOf(timeUnits)

  val genPositiveMilliseconds: Gen[Milliseconds] = for {
    magnitude <- Gen.posNum[Long]
  } yield {
    Milliseconds(magnitude)
  }

  private val httpResponseCodes = Seq[HttpResponseCode](Accepted,
    BadGateway,
    MethodNotAllowed,
    BadRequest,
    ClientTimeout,
    Conflict,
    Created,
    EntityTooLarge,
    FailedDependency,
    Forbidden,
    GatewayTimeout,
    Gone,
    InsufficientStorage,
    InternalServerError,
    LengthRequired,
    Locked,
    MovedPermanently,
    TemporaryRedirect,
    MultipleChoices,
    MultiStatus,
    NoContent,
    NotAcceptable,
    NonAuthoritativeInformation,
    NotFound,
    NotImplemented,
    NotModified,
    Ok,
    PartialContent,
    PaymentRequired,
    PreconditionFailed,
    AuthenticationRequired,
    RequestURITooLarge,
    ResetContent,
    SeeOther,
    Unauthorized,
    ServiceUnavailable,
    UnprocessableEntity,
    UnsupportedMediaType,
    UseProxy,
    HttpVersionNotSupported
  )
  val genHttpResponseCode: Gen[HttpResponseCode] = Gen.oneOf(httpResponseCodes)


  val genHashCode: Gen[HashCode] = for {
    str <- genNonEmptyString
  } yield {
    str.getBytes
  }

  val genRawBody: Gen[RawBody] = genHashCode

  val genCachedResponseDelay = for {
    ttl <- genPositiveMilliseconds
    hashCode <- genHashCode
  } yield {
    CachedResponseDelay(ttl, hashCode)
  }

  val genHeader: Gen[Header] = for {
    key <- genNonEmptyString
    value <- genNonEmptyString
  } yield {
    key -> value
  }

  val genHeaders: Gen[Headers] = for {
    headers <- Gen.listOf(genHeader)
  } yield {
    Headers(headers)
  }

  val genURL: Gen[URL] = for {
    urlString <- genNonEmptyString
  } yield {
    new URL("http://%s.com".format(urlString))
  }

  def genHttpRequest(client: HttpClient): Gen[HttpRequest] = for {
    url <- genURL
    headers <- genHeaders
  } yield {
    client.get(url, headers)
  }

  val genHttpResponse: Gen[HttpResponse] = for {
    code <- genHttpResponseCode
    headers <- genHeaders
    body <- genRawBody
  } yield {
    new HttpResponse(code, headers, body)
  }

  val genCache: Gen[HttpResponseCacher] = {
    Gen.value(new InMemoryHttpResponseCacher)
  }

}
