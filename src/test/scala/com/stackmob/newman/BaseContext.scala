package com.stackmob.newman

import com.stackmob.common.logging.LoggingSugar
import com.stackmob.newman.request.HttpRequest.Headers
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
  protected sealed abstract class HeadersBaseMatcher(protected val givenHeaders: Headers) extends Matcher[Headers]
  protected class HeadersAreEqualMatcher(expected: Headers) extends HeadersBaseMatcher(expected) {
    override def apply[S <: Headers](r: Expectable[S]): MatchResult[S] = {
      val other: Headers = r.value
      val res = (expected, other) match {
        case (Some(h1), Some(h2)) => h1.list === h2.list
        case (None, None) => true
        case _ => false
      }
      result(res, "Headers are equal", expected + " does not equal " + other, r)
    }
  }

  protected def haveTheSameHeadersAs(h: Headers) = new HeadersAreEqualMatcher(h)
}
