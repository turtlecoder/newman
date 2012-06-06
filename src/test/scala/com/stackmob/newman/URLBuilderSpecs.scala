package com.stackmob.newman

import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import java.net.URL

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 6/6/12
 * Time: 2:42 PM
 */

class URLBuilderSpecs extends Specification { def is =
  "URLBuilderSpecs".title                                                                                               ^
  """
  URLBuilder is Newman's DSL for building URLs in as much of a typesafe way as possible
  """                                                                                                                   ^
  "URLBuilder should"                                                                                                   ^
    "correctly assemble a basic http://something.something URL"                                                         ! ProtocolAndHost().succeeds ^
    "correctly assemble an http://something.something/something URL"                                                    ! ProtocolHostAndPath().succeeds ^
    "correctly assemble an http://something.somethign/something?key=val URL"                                            ! ProtocolHostPathAndQueryString().succeeds ^
                                                                                                                        end
  trait Context extends BaseContext with URLBuilderDSL {
    protected val defaultHost = "stackmob.com"
    protected def baseURLMatches(u: URL, protocol: Protocol = Http,
                                 host: String = defaultHost,
                                 port: Int = DefaultPort): SpecsResult = {
      (u.getProtocol must beEqualTo(Http.name)) and
      (u.getHost must beEqualTo(defaultHost)) and
      (u.getPort must beEqualTo(DefaultPort))
    }
  }

  case class ProtocolAndHost() extends Context {
    def succeeds:SpecsResult = baseURLMatches(Http |:/| defaultHost)
  }

  case class ProtocolHostAndPath() extends Context {
    def succeeds: SpecsResult = {
      val path = "path"
      val u: URL = Http |:/| defaultHost |/| path
      baseURLMatches(u) and (u.getPath must beEqualTo("/%s".format(path)))
    }
  }

  case class ProtocolHostPathAndQueryString() extends Context {
    def succeeds: SpecsResult = {
      val path = "path"
      val queryString1Key = "key1"
      val queryString1Value = "val1"
      val queryString2Key = "key2"
      val queryString2Value = "val2"

      val u: URL = Http |:/| defaultHost |/| path |?| (queryString1Key |=| queryString1Value) |&| (queryString2Key |=| queryString2Value)
      val queryStringMatches: SpecsResult = u.getQuery.split("&").length must beEqualTo(2)
      baseURLMatches(u) and (u.getPath must beEqualTo("/%s".format(path))) and queryStringMatches
    }
  }

}
