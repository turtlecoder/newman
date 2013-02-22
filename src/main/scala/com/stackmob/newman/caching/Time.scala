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

import java.util.concurrent.TimeUnit

sealed trait Time {
  def magnitude: Long
  def unit: TimeUnit

  /**
   * get this time in a different unit
   * @param newUnit the new unit
   * @return this time in newUnit
   */
  def asUnit(newUnit: TimeUnit): Time = {
    Time(newUnit.convert(magnitude, unit), newUnit)
  }

  /**
   * add a time to this one
   * @param otherTime the time to add
   * @return the new time
   */
  def ++(otherTime: Time): Time = {
    val otherAsThisUnit = otherTime.asUnit(unit)
    val newMagnitude = otherAsThisUnit.magnitude + this.magnitude
    Time(newMagnitude, unit)
  }

  /**
   * subtract a time from this one
   * @param otherTime the time to subtract
   * @return the new time
   */
  def --(otherTime: Time): Time = {
    this ++ Time(otherTime.magnitude * -1, otherTime.unit)
  }
}


object Time {
  def apply(mag: Long, u: TimeUnit) = new Time {
    override lazy val magnitude: Long = mag
    override lazy val unit = u
  }
  def now = apply(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
}
