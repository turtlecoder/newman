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

package com.stackmob.newman.test.concurrent

import org.specs2.Specification
import scalaz.concurrent.Promise
import com.stackmob.newman.RichPromise
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RichPromiseSpecs extends Specification { def is =
  "RichScalaFutureSpecs".title                                                                                          ^ end ^
  "RichPromise is a class extension for scalaz.concurrent.Promise"                                                      ^ end ^
  "except should work properly"                                                                                         ! except ^ end ^
  "toScalaFuture should work properly"                                                                                  ! toScalaFuture ^ end ^
  end

  private def except = {
    val ex = new Exception("test exception")
    val throwingPromise = Promise {
      (throw ex): String
    }.except { t =>
      t.getMessage
    }

    throwingPromise.get must beEqualTo(ex.getMessage)
  }

  private def toScalaFuture = {
    val value = 12345
    val prom = Promise {
      value
    }
    Await.result(prom.toScalaFuture, Duration.Inf) must beEqualTo(value)
  }
}
