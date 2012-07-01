package com.stackmob.newman
package dsl

import java.net.URL

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 6/6/12
 * Time: 2:09 PM
 */

trait URLBuilderDSL {
  import Path._

  val DefaultPort = 80

  implicit def urlCapableToURL(c: URLCapable) = c.toURL
  implicit def stringToPath(s: String) = Path(s :: Nil)

  sealed trait Protocol {
    def name: String
  }
  case object http extends Protocol {
    override val name = "http"
  }
  case object https extends Protocol {
    override val name = "https"
  }

  def url(protocol: Protocol, host: String, port: Int, path: Path): PathBuilder = PathBuilder(protocol, host, port, path)
  def url(protocol: Protocol, host: String, path: Path): PathBuilder = url(protocol, host, DefaultPort, path)
  def url(protocol: Protocol, host: String, port: Int): PathBuilder = url(protocol, host, port, empty)
  def url(protocol: Protocol, host: String): PathBuilder = url(protocol, host, DefaultPort, empty)
  def url(host: String): PathBuilder = url(http, host, DefaultPort, empty)

  trait URLCapable {
    def protocol: Protocol
    def host: String
    def port: Int
    def path: Path
    def query: List[(String, String)] = Nil

    def toURL: URL = {
      val queryString = if (query.length > 0) {
        "?%s".format(query.map(elt => "%s=%s".format(elt._1, elt._2)).mkString("&"))
      } else {
        ""
      }
      new URL("%s://%s:%d/%s%s".format(protocol.name, host, port, path.toString, queryString))
    }
  }

  case class PathBuilder(protocol: Protocol,
                         host: String,
                         port: Int,
                         path: Path = Path.empty) extends URLCapable {
    def /(pathElt: String) = PathBuilder(protocol, host, port, path / pathElt)
    def /(path: Path) = PathBuilder(protocol, host, port, path)
    def ?(queryStringElts: (String, String)*) = {
      QueryStringBuilder(protocol, host, port, path, queryStringElts.toList)
    }
  }

  case class QueryStringBuilder(protocol: Protocol,
                                host: String,
                                port: Int,
                                path: Path,
                                override val query: List[(String, String)] = Nil) extends URLCapable {
    def &(queryStringElt: (String, String)): QueryStringBuilder = {
      QueryStringBuilder(protocol, host, port, path, query :+ queryStringElt)
    }
  }
}

case class Path(list: List[String]) {
  def /(s: String) = copy(list :+ s)
  def /(p: Path) = copy(list ++ p.list)
  override def toString = list.mkString("/")
}

object Path {
  val empty = Path(Nil)
}

