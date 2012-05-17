package com.stackmob.newman

import com.stackmob.common.logging.LoggingSugar
import com.stackmob.newman.request.HttpRequest.Headers
import com.stackmob.newman.request.HttpRequest.Headers.HeadersEqual
import com.stackmob.newman.response.HttpResponse
import org.specs2.matcher.{MatchResult, Expectable, Matcher}
import scalaz._
import Scalaz._

/**
 * Created by IntelliJ IDEA.
 *
 *
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 3:30 PM
 */

trait BaseContext extends LoggingSugar {

  protected class HeadersAreEqualMatcher(expected: Headers) extends Matcher[Headers] {
    override def apply[S <: Headers](r: Expectable[S]): MatchResult[S] = {
      val other: Headers = r.value
      val res = expected === other
      result(res, "Headers are equal", expected + " does not equal " + other, r)
    }
  }

  protected class HttpResponsesAreEqualMatcher(expected: HttpResponse) extends Matcher[HttpResponse] {
    override def apply[S <: HttpResponse](r: Expectable[S]): MatchResult[S] = {
      val other: HttpResponse = r.value
      val res = (expected.code === other.code) && (expected.headers === other.headers) && (expected.body == other.body)
      result(res, "HttpResponses are equal", expected + " does not equal " + other, r)
    }
  }


  protected def haveTheSameHeadersAs(h: Headers) = new HeadersAreEqualMatcher(h)

  protected def beTheSameResponseAs(h: HttpResponse) = new HttpResponsesAreEqualMatcher(h)
}
