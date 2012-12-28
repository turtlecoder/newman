package com.stackmob.newman

import com.stackmob.newman.request.HttpRequest.Headers
import com.stackmob.newman.request.HttpRequest.Headers.HeadersEqual
import com.stackmob.newman.response.HttpResponse
import org.specs2.matcher.{MatchResult, Expectable, Matcher}
import org.specs2.execute.{Failure => SpecsFailure, Result => SpecsResult}
import scalaz._
import Scalaz._
import net.liftweb.json.scalaz.JsonScalaz._
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 *
 *
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 3:30 PM
 */

trait BaseContext {

  private lazy val logger = LoggerFactory.getLogger(classOf[BaseContext])

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
      val res = (expected.code === other.code) && (expected.headers === other.headers) && (expected.bodyString === other.bodyString)
      result(res, "HttpResponses are equal", expected + " does not equal " + other, r)
    }
  }


  protected def haveTheSameHeadersAs(h: Headers) = new HeadersAreEqualMatcher(h)

  protected def beTheSameResponseAs(h: HttpResponse) = new HttpResponsesAreEqualMatcher(h)

  protected def logAndFail(t: Throwable): SpecsResult = {
    logger.warn(t.getMessage, t)
    SpecsFailure("failed with exception %s".format(t.getMessage))
  }

  private def errorString(err: Error) = err match {
    case UnexpectedJSONError(was, expected) => "unexpected JSON. was %s, expected %s".format(was.toString, expected.toString)
    case NoSuchFieldError(name, json) => "no such field %s in json %s".format(name, json.toString)
    case UncategorizedError(key, desc, args) => "uncategorized JSON error for key %s: %s (args %s)".format(key, desc, args.mkString("&"))
  }

  protected def logAndFail(errs: NonEmptyList[Error]): SpecsResult = {
    val totalErrString = errs.map(errorString(_)).list.mkString("\n")
    logger.warn("JSON errors:\n%s".format(totalErrString))
    SpecsFailure("JSON errors occurred: %s".format(totalErrString))
  }
}
