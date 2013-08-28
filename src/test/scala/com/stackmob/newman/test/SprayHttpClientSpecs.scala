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
import com.stackmob.newman.SprayHttpClient
import akka.actor.ActorSystem
import org.specs2.specification.Around
import org.specs2.execute.AsResult

class SprayHttpClientSpecs extends Specification with ClientTests with ResponseMatcher { def is =
  "SprayHttpClientSpecs".title                                                                                          ^ end ^
  "SprayHttpClient is a spray-based nonblocking HTTP client"                                                            ^ end ^
  "get should work"                                                                                                     ! test(_.get) ^
  "getAsync should work"                                                                                                ! test(_.getAsync) ^
  "post should work"                                                                                                    ! test(_.post) ^ end ^
  "postAsync should work"                                                                                               ! test(_.postAsync) ^ end ^
  "put should work"                                                                                                     ! test(_.put) ^ end ^
  "putAsync should work"                                                                                                ! test(_.putAsync) ^ end ^
  "delete should work"                                                                                                  ! test(_.delete) ^ end ^
  "deleteAsync should work"                                                                                             ! test(_.deleteAsync) ^ end ^
  "head should work"                                                                                                    ! test(_.head) ^ end ^
  "headAsync should work"                                                                                               ! test(_.headAsync) ^ end ^
  end

  private def test[T](fn: ClientTests => T) = {
    val system = ActorSystem("SprayHttpClientSpecs")
    val client = new SprayHttpClient(ActorSystem("SprayHttpClientSpecs"))
    val clientTests = ClientTests(client)
    val res = fn(clientTests)
    system.shutdown()
    res
  }
}
