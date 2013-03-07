/**
 * Copyright 2012 StackMob
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

package com.stackmob.newman.test.caching

import org.specs2.{ScalaCheck, Specification}
import org.scalacheck._
import Prop._
import com.stackmob.newman.test.scalacheck._
import java.util.concurrent.TimeUnit
import com.stackmob.newman.caching.{CachedResponseDelay, Milliseconds}

class CachedResponseDelaySpecs extends Specification with ScalaCheck { def is =
  "CachedResponseDelaySpecs".title                                                                                      ^ end ^
  """
  CachedResponse is a Delayed subclass, which is used to schedule cached responses for expiry
  """                                                                                                                   ^ end ^
  "getDelay should be correct"                                                                                          ! getDelayIsCorrect ^ end ^
  "compareTo should be correct"                                                                                         ! compareToIsCorrect ^ end

  private def getDelayIsCorrect = forAll(genHashCode) { (hashCode) =>
    val crd = CachedResponseDelay(Milliseconds(0), hashCode)
    crd.getDelay(TimeUnit.MILLISECONDS) must beLessThanOrEqualTo(0L)
  }

  private def compareToIsCorrect = forAll(genHashCode, genHashCode) { (h1, h2) =>
    val crd1 = CachedResponseDelay(Milliseconds(0), h1)
    val crd2 = CachedResponseDelay(Milliseconds(1000), h2)
    crd1.compareTo(crd2) must beEqualTo(-1)
  }

}
