/**
 * Copyright 2012-2013 StackMob
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

package com.stackmob

import scalaz._
import Scalaz._
import java.nio.charset.Charset

package object newman extends NewmanPrivate {

  type Header = (String, String)
  type HeaderList = NonEmptyList[Header]
  type Headers = Option[HeaderList]

  object Headers {
    implicit val HeadersEqual = new Equal[Headers] {
      override def equal(headers1: Headers, headers2: Headers): Boolean = (headers1, headers2) match {
        case (Some(h1), Some(h2)) => h1.list === h2.list
        case (None, None) => true
        case _ => false
      }
    }

    implicit val HeadersZero = new Zero[Headers] {
      override val zero = Headers.empty
    }

    implicit val HeadersShow = new Show[Headers] {
      override def show(h: Headers): List[Char] = {
        val s = h.map { headerList: HeaderList =>
          headerList.list.map(h => "%s=%s".format(h._1, h._2)).mkString("&")
        } | ""
        s.toList
      }
    }

    def apply(h: Header): Headers = Headers(nel(h))
    def apply(h: Header, tail: Header*): Headers = Headers(nel(h, tail.toList))
    def apply(h: HeaderList): Headers = h.some
    def apply(h: List[Header]): Headers = h.toNel
    def empty: Option[HeaderList] = Option.empty[HeaderList]
  }

  type RawBody = Array[Byte]
  implicit val RawBodyZero = new Zero[RawBody] {
    override lazy val zero = Array[Byte]()
  }
  object RawBody {
    def apply(s: String, charset: Charset = Constants.UTF8Charset): Array[Byte] = s.getBytes(charset)
    def apply(b: Array[Byte]): Array[Byte] = b
    lazy val empty = Array[Byte]()
  }
}
