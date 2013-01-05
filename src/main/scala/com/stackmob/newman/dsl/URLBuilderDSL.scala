/**
 * Copyright 2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.newman
package dsl

import java.net.URL

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
      val pathString = path.toString
      // add a slash if the path exists and doesn't already have one
      val slash = if(pathString.isEmpty || pathString.startsWith("/")) "" else "/"
      new URL("%s://%s:%d%s%s%s".format(protocol.name, host, port, slash, pathString, queryString))
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

