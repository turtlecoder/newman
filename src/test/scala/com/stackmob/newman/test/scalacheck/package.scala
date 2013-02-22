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

import com.stackmob.newman.caching._
import org.scalacheck.Gen
import java.util.concurrent.TimeUnit

package object scalacheck {
  val genNonEmptyString: Gen[String] = for {
    firstChar <- Gen.listOf1(Gen.alphaChar)
    remainingChars <- Gen.listOf(Gen.alphaChar)
  } yield {
    val charList = List(firstChar) ++ remainingChars
    charList.mkString
  }

  private val timeUnits = Seq(TimeUnit.DAYS,
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

  val genHashCode: Gen[HashCode] = for {
    str <- genNonEmptyString
  } yield {
    str.getBytes
  }

  val genCachedResponseDelay = for {
    ttl <- genPositiveMilliseconds
    hashCode <- genHashCode
  } yield {
    CachedResponseDelay(ttl, hashCode)
  }

}
