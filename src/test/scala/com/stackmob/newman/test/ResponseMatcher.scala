package com.stackmob.newman.test

import org.specs2.Specification
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import org.specs2.matcher.{Expectable, Matcher}
import com.stackmob.newman.{Headers, RawBody}

trait ResponseMatcher { this: Specification =>
  class ResponseMatcher(expectedCode: HttpResponseCode,
                        mbHeaders: Option[Headers] = None,
                        mbBody: Option[RawBody] = None) extends Matcher[HttpResponse] {
    def apply[S <: HttpResponse](expectable: Expectable[S]) = {
      val actualResp = expectable.value
      val descr = expectable.description
      val codeRes = actualResp.code must beEqualTo(expectedCode)
      result(codeRes,
        s"$descr matches code $expectedCode",
        s"$descr does not match code $expectedCode",
        expectable)
    }
  }

  def beResponse(c: HttpResponseCode,
                 mbHeaders: Option[Headers] = None,
                 mbBody: Option[RawBody] = None) = {
    new ResponseMatcher(c, mbHeaders, mbBody)
  }
}
