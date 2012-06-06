package com.stackmob.newman

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
  val DefaultPort = 80
  implicit def stringToQueryStringEltBuilder(s: String) = QueryStringEltBuilder(s)
  implicit def protocolToHostBuilder(protocol: Protocol) = HostBuilder(protocol)
  implicit def urlCapableToURL(c: URLCapable) = c.toURL

  sealed trait Protocol {
    def name: String
  }
  case object Http extends Protocol {
    override val name = "http"
  }
  case object Https extends Protocol {
    override val name = "https"
  }

  case class HostBuilder(protocol: Protocol) {
    def |:/|(host: String): PortBuilder = PortBuilder(protocol, host)
  }

  case class PortBuilder(protocol: Protocol,
                         host: String) extends URLCapable {
    def |:|(port: Int) = PathBuilder(protocol, host, port)
    def |/|(pathElt: String) = PathBuilder(protocol, host, DefaultPort, pathElt :: Nil)
    override def toURL = new URL("%s://%s:%d".format(protocol.name, host, DefaultPort))
  }

  trait URLCapable {
    def toURL: URL
  }

  case class PathBuilder(protocol: Protocol,
                         host: String,
                         port: Int,
                         pathElts: List[String] = Nil) extends URLCapable {
    def |/|(pathElt: String) = PathBuilder(protocol, host, port, pathElts :+ pathElt)
    def |?|(queryStringElt: QueryStringElt) = QueryStringBuilder(protocol,
      host,
      port,
      pathElts,
      queryStringElt :: Nil)

    override def toURL = new URL("%s://%s:%d/%s".format(protocol.name, host, port, pathElts.mkString("/")))
  }

  case class QueryStringElt(key: String, value: String)
  case class QueryStringEltBuilder(key: String) {
    def |=|(value: String) = QueryStringElt(key, value)
  }

  case class QueryStringBuilder(protocol: Protocol,
                                host: String,
                                port: Int,
                                pathElts: List[String],
                                queryStringElts: List[QueryStringElt] = Nil) extends URLCapable {
    def |&|(queryStringElt: QueryStringElt) = QueryStringBuilder(protocol,
      host,
      port,
      pathElts,
      queryStringElts :+ queryStringElt)

    override def toURL = {
      val queryString = {
        val q = queryStringElts.map(elt => "%s=%s".format(elt.key, elt.value)).mkString("&")
        if(q.length > 0) {
          "?%s".format(q)
        }
        else {
          ""
        }
      }
      new URL("%s://%s:%d/%s%s".format(protocol.name, host, port, pathElts.mkString("/"), queryString))
    }
  }
}
