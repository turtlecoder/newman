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

sealed trait Milliseconds extends Comparable[Milliseconds] {
  def magnitude: Long
  override def compareTo(other: Milliseconds) = {
    magnitude.compareTo(other.magnitude)
  }

  def ++(other: Milliseconds): Milliseconds = {
    Milliseconds(magnitude + other.magnitude)
  }
}
object Milliseconds {
  /**
   * get the current time
   * @return the current number of milliseconds since the epoch
   */
  def current: Milliseconds = apply(System.currentTimeMillis())

  /**
   * create a new number of milliseconds
   * @param l the number of milliseconds
   * @return a Milliseconds object
   */
  def apply(l: Long): Milliseconds = new Milliseconds {
    override lazy val magnitude: Long = l
  }
}