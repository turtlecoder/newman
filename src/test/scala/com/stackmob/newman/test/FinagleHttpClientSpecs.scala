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
import com.stackmob.newman.FinagleHttpClient

class FinagleHttpClientSpecs extends Specification with ClientTests with ResponseMatcher { def is =
  "FinagleHttpClientSpecs".title                                                                                        ^ end ^
  "FinagleHttpClient is a finagle-based nonblocking HTTP client"                                                        ^ end ^
  "get should work"                                                                                                     ! ClientTests(client).get ^
  "getAsync should work"                                                                                                ! ClientTests(client).getAsync ^
  "post should work"                                                                                                    ! ClientTests(client).post ^ end ^
  "postAsync should work"                                                                                               ! ClientTests(client).postAsync ^ end ^
  "put should work"                                                                                                     ! ClientTests(client).put ^ end ^
  "putAsync should work"                                                                                                ! ClientTests(client).putAsync ^ end ^
  "delete should work"                                                                                                  ! ClientTests(client).delete ^ end ^
  "deleteAsync should work"                                                                                             ! ClientTests(client).deleteAsync ^ end ^
  "head should work"                                                                                                    ! ClientTests(client).head ^ end ^
  "headAsync should work"                                                                                               ! ClientTests(client).headAsync ^ end ^
  end
  private def client = new FinagleHttpClient
}
