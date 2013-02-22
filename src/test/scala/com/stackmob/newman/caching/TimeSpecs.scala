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

package com.stackmob.newman.caching

import org.specs2.{ScalaCheck, Specification}
import java.util.concurrent.TimeUnit
import org.scalacheck._
import Prop._
import com.stackmob.newman.scalacheck._

class TimeSpecs extends Specification with ScalaCheck { def is =
  "TimeSpecs".title                                                                                                     ^ end ^
  "Time is a data structure that manages times in various units"                                                        ^ end ^
  "Time.asUnit returns the current time in the new unit"                                                                ! asUnitWorks ^ end ^
  "Time.add works as expected"                                                                                          ! addWorks ^ end ^
  "Time.subtract works as expected"                                                                                     ! subtractWorks ^ end


  private val microUnit = TimeUnit.MICROSECONDS

  private def asUnitWorks = forAll(genPositiveTime, genTimeUnit) { (time, otherUnit) =>
    val converted = otherUnit.convert(time.magnitude, time.unit)
    val asUnit = time.asUnit(otherUnit)
    asUnit.magnitude must beEqualTo(converted)
  }

  private def addWorks = forAll(genPositiveTime, genPositiveTime) { (time1, time2) =>
    val addRes = time1 ++ time2
    val expectedMicroseconds = time1.asUnit(microUnit).magnitude + time2.asUnit(microUnit).magnitude

    addRes.asUnit(microUnit).magnitude must beEqualTo(expectedMicroseconds)
  }

  private def subtractWorks = forAll(genPositiveTime, genPositiveTime) { (time1, time2) =>
    val subRes = time1 -- time2
    val expectedMicroseconds = time1.asUnit(microUnit).magnitude - time2.asUnit(microUnit).magnitude
    subRes.asUnit(microUnit).magnitude must beEqualTo(expectedMicroseconds)
  }
}
