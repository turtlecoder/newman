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

import org.specs2.Specification
import java.util.concurrent.TimeUnit

class TimeSpecs extends Specification { def is =
  "TimeSpecs".title                                                                                                     ^
  "Time is a data structure that manages times in various units"                                                        ^
  "Time.asUnit returns the current time in the new unit"                                                                ! asUnitWorks ^ end ^
  "Time.add works as expected"                                                                                          ! addWorks ^ end ^
  "Time.subtract works as expected"                                                                                     ! subtractWorks ^ end


  private def asUnitWorks = {
    val nowInMilliseconds = Time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    val nowInSeconds = nowInMilliseconds.asUnit(TimeUnit.SECONDS)
    nowInSeconds.magnitude must beEqualTo(nowInMilliseconds.magnitude / 1000)
  }

  private def addWorks = {
    val now = Time.now
    val moreSeconds = Time(2, TimeUnit.SECONDS)
    val res = now ++ moreSeconds
    (res.asUnit(TimeUnit.SECONDS).magnitude - 2) must beEqualTo(now.asUnit(TimeUnit.SECONDS).magnitude)
  }

  private def subtractWorks = {
    val now = Time.now
    val fewerSeconds = Time(2, TimeUnit.SECONDS)
    val res = now -- fewerSeconds
    (res.asUnit(TimeUnit.SECONDS).magnitude + 2) must beEqualTo(now.asUnit(TimeUnit.SECONDS).magnitude)
  }


}
