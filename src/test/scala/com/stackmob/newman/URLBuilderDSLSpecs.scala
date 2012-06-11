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

class URLBuilderDSLSpecs extends Specification { def is =
  "URLBuilderDSLSpecs".title                                                                                            ^
  """
  URLBuilder is Newman's DSL for building URLs in as much of a typesafe way as possible
  """                                                                                                                   ^
  "URLBuilder should"                                                                                                   ^
    "correctly assemble a basic http://something.something URL"                                                         ! ProtocolAndHost().succeeds ^
    "correctly assemble an http://something.something/something URL"                                                    ! ProtocolHostAndPath().succeeds ^
    "correctly assemble an http://something.somethign/something?key=val URL"                                            ! ProtocolHostPathAndQueryString().succeeds ^
    "correctly assemble an http://something.something?key=val URL"                                                      ! ProtocolHostAndQueryString().succeeds ^
                                                                                                                        end
  trait Context extends BaseContext with URLBuilderDSL {
    protected val protocol = http
    protected val host = "stackmob.com"
    protected val port = DefaultPort

    protected def baseURLMatches(u: URL,
                                 protocol: Protocol = http,
                                 host: String = host,
                                 port: Int = port): SpecsResult = {
      (u.getProtocol must beEqualTo(http.name)) and
      (u.getHost must beEqualTo(host)) and
      (u.getPort must beEqualTo(port))
    }

    protected def checkQueryString(u: URL,
                                   expected: List[(String, String)]): SpecsResult = {
      val givenQueryList = u.getQuery.split("&").flatMap { elt: String =>
        elt.split("=").toList match {
          case k :: v :: Nil => List(k -> v)
          case _ => Nil
        }
      }.toList
      expected must haveTheSameElementsAs(givenQueryList)
    }
  }

  case class ProtocolAndHost() extends Context {
    def succeeds:SpecsResult = baseURLMatches(url(http, host))
  }

  case class ProtocolHostAndPath() extends Context {
    def succeeds: SpecsResult = {
      val path = "path1" / "path2"
      val u: URL = url(http, host) / path
      baseURLMatches(u) and (u.getPath must beEqualTo("/%s".format(path.toString)))
    }
  }

  case class ProtocolHostPathAndQueryString() extends Context {
    def succeeds: SpecsResult = {
      val path = "path"
      val k1 = "key1"
      val v1 = "val1"
      val k2 = "key2"
      val v2 = "val2"

      val u: URL = url(http, host, path) ? (k1 -> v1) & (k2 -> v2) & (k1 -> v1)
      baseURLMatches(u) and checkQueryString(u, List(k1 -> v1, k2 -> v2, k1 -> v1))
    }
  }

  case class ProtocolHostAndQueryString() extends Context {
    def succeeds: SpecsResult = {
      val q1Key = "key1"
      val q2Key = "key2"

      val u: URL = url(http, host) ? (q1Key -> q2Key)
      baseURLMatches(u) and checkQueryString(u, List(q1Key -> q2Key))
    }
  }

}
