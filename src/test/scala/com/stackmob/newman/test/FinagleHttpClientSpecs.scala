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
