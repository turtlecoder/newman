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
import java.net.URL
import com.stackmob.newman.response.{HttpResponse, HttpResponseCode}
import com.stackmob.newman._
import com.stackmob.newman.dsl._
import org.specs2.matcher.MatchResult

class ApacheHttpClientSpecs extends Specification with ClientTests with ResponseMatcher { def is =
  "ApacheHttpClientSpecs".title                                                                                         ^ end ^
  "ApacheHttpClient is the HttpClient implementation that actually hits the internet"                                   ^ end ^
  "get should work"                                                                                                     ! ClientTests(httpClient).get ^ end ^
  "getAsync should work"                                                                                                ! ClientTests(httpClient).getAsync ^ end ^
  "post should work"                                                                                                    ! ClientTests(httpClient).post ^ end ^
  "postAsync should work"                                                                                               ! ClientTests(httpClient).postAsync ^ end ^
  "put should work"                                                                                                     ! ClientTests(httpClient).put ^ end ^
  "putAsync should work"                                                                                                ! ClientTests(httpClient).putAsync ^ end ^
  "delete should work"                                                                                                  ! ClientTests(httpClient).delete ^ end ^
  "deleteAsync should work"                                                                                             ! ClientTests(httpClient).deleteAsync ^ end ^
  "head should work"                                                                                                    ! ClientTests(httpClient).head ^ end ^
  "headAsync should work"                                                                                               ! ClientTests(httpClient).headAsync ^ end ^
  end
  private def httpClient = new ApacheHttpClient
}
