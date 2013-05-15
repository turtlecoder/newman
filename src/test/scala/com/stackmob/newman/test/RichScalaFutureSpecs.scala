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
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.stackmob.newman.SprayHttpClient
import SprayHttpClient.RichScalaFuture

class RichScalaFutureSpecs extends Specification { def is =
  "RichScalaFutureSpecs".title                                                                                          ^ end ^
  "RichScalaFuture is a class extension for scala.concurrent.Future"                                                    ^ end ^
  "toScalazPromise should work properly"                                                                                ! toScalazPromise ^ end ^
  end

  private def toScalazPromise = {
    val futureReturn = 1
    val fut = future {
      futureReturn
    }
    val prom = fut.toScalazPromise
    prom.get must beEqualTo(futureReturn)
  }
}
