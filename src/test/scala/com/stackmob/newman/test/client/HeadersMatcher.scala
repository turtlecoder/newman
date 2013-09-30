package com.stackmob.newman.test.client

import org.specs2.matcher.{Expectable, MustMatchers, MustExpectations, Matcher, MatchResult}
import org.specs2.Specification
import com.stackmob.newman.{Headers, HeaderList}
import com.stackmob.newman.Headers.HeadersShow
import scalaz.Scalaz._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.test.client
 *
 * User: aaron
 * Date: 9/27/13
 * Time: 5:24 PM
 */
trait HeadersMatcher extends MustExpectations with MustMatchers { this: Specification =>

  def containHeaders(containHeaders: Headers): ContainHeadersMatcher = {
    new ContainHeadersMatcher(containHeaders)
  }

  def containHeaders(containHeaders: Map[String, String]): ContainHeadersMatcher = {
    val hdrs = Headers(containHeaders.toList)
    new ContainHeadersMatcher(hdrs)
  }

  class ContainHeadersMatcher(expectedContainHeaders: Headers) extends Matcher[Headers] {
    def apply[S <: Headers](expectable: Expectable[S]): MatchResult[S] = {
      val actualHeaders = expectable.value

      //actualHeaders and expectedHeaders must both be some or both be none
      val res = (expectedContainHeaders tuple actualHeaders) must beSome.like {
        case (expectedContainHeaderList: HeaderList, actualHeaderList: HeaderList) => {
          actualHeaderList.list.mkString(",") must contain(expectedContainHeaderList.list.mkString(","))
        }
      } unless {
        val expectedRes = !expectedContainHeaders.isDefined
        val actualRes = !actualHeaders.isDefined
        expectedRes && actualRes
      }
      val descr = expectable.description
      val headersStr = expectedContainHeaders.shows
      result(res,
        s"$descr contains given headers $headersStr",
        s"$descr does not contain given headers $headersStr",
        expectable)
    }

  }

}
