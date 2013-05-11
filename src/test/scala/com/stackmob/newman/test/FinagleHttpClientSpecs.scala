package com.stackmob.newman.test

import org.specs2.Specification
import com.stackmob.newman.FinagleHttpClient
import java.net.URL
import com.stackmob.newman.HeaderList
import com.stackmob.newman.response.HttpResponseCode

class FinagleHttpClientSpecs extends Specification { def is =
  "FinagleHttpClientSpecs".title                                                                                        ^ end ^
  "FinagleHttpClient is a finagle-based nonblocking HTTP client"                                                        ^ end ^
  "get should work"                                                                                                     ! get ^ end ^
  end

  private lazy val getUrl = new URL("http://httpbin.org/get")
  private lazy val headers = Option.empty[HeaderList]
  private lazy val client = new FinagleHttpClient
  private def get = {
    val resp = client.get(getUrl, headers).executeUnsafe
    val codeRes = resp.code must beEqualTo(HttpResponseCode.Ok)
    codeRes
  }

}
