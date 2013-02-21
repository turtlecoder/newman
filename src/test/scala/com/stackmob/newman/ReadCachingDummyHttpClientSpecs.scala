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

package com.stackmob.newman

import org.specs2.Specification

class ReadCachingDummyHttpClientSpecs extends Specification { def is =
  "ReadCachingDummyHttpClientSpecs".title                                                                               ^ end ^
  "CachingDummyHttpClient is an HttpClient that caches responses for some defined TTL"                                  ^ end ^
  "get should read from the cache if there is an entry already"                                                         ! skipped ^ end ^
  "get should read from the client if no cache entry, and add to the cache"                                             ! skipped ^ end ^
  "head should read from the cache if there is a cache entry already"                                                   ! skipped ^ end ^
  "head should read from the client if no cache entry, and add to the cache"                                            ! skipped ^ end ^
  "POST, PUT, DELETE should not touch the cache"                                                                        ! skipped ^ end
}
