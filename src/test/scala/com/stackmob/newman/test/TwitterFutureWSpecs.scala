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

package com.stackmob.newman.test

import org.specs2.Specification
import com.twitter.util._
import com.stackmob.newman.FinagleHttpClient._

class TwitterFutureWSpecs extends Specification { def is =
  "TwitterFutureSpecs".title                                                                                            ^ end ^
  "TwitterFutureW is a class extension for com.twitter.util.Future"                                                     ^ end ^
  "toScalaPromise should work properly"                                                                                 ! toScalaPromise ^ end ^
  end

  private def toScalaPromise = {
    val futureReturn = 1
    val fut = Future {
      futureReturn
    }

    val prom = fut.toScalaPromise
    prom.get must beEqualTo(futureReturn)
  }
}
