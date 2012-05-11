package com.stackmob.newman.request

import java.net.URL
import scalaz.NonEmptyList
import scalaz._
import Scalaz._
import scalaz.effects._
import com.stackmob.newman.response._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.barney.service.filters.proxy
 *
 * User: aaron
 * Date: 4/24/12
 * Time: 4:58 PM
 */


trait HttpRequest {
  import HttpRequest._
  def url: URL

  def headers: Headers

  def execute: IO[HttpResponse]
}

object HttpRequest {
  type Header = (String, String)
  type HeaderList = NonEmptyList[Header]
  type Headers = Option[HeaderList]

  object Headers {
    def apply(h: Header): Headers = nel(h).some
    def apply(h: Header, tail: Header*): Headers = nel(h, tail.toList).some
    def apply(h: HeaderList): Headers = h.some
    def apply(h: List[Header]): Headers = h.toNel
    def empty = Option.empty[HeaderList]
  }
}
