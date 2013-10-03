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

package com.stackmob.newman.test.client

import org.specs2.Specification
import com.stackmob.newman.FinagleHttpClient
import com.twitter.util.Duration

class FinagleHttpClientSpecs extends Specification with ClientTests { def is =
  "FinagleHttpClientSpecs".title                                                                                        ^ end ^
  "FinagleHttpClient is a finagle-based nonblocking HTTP client"                                                        ^ end ^
  "get should work"                                                                                                     ! ClientTests(client).get ^
  "post should work"                                                                                                    ! ClientTests(client).post ^ end ^
  "put should work"                                                                                                     ! ClientTests(client).put ^ end ^
  "delete should work"                                                                                                  ! ClientTests(client).delete ^ end ^
  "head should work"                                                                                                    ! ClientTests(client).head ^ end ^
  end
  private val oneSecond = Duration.fromSeconds(1)
  private def client = new FinagleHttpClient(tcpConnectionTimeout = oneSecond, requestTimeout = oneSecond)
}
