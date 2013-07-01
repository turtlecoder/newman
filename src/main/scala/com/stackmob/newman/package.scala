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
import scalaz.effect.IO
import scalaz.concurrent.Promise
import scalaz.NonEmptyList._
import scalaz.concurrent.{Promise => ScalazPromise}
import scala.concurrent.{Promise => ScalaPromise, Future => ScalaFuture}
import Scalaz._
import java.nio.charset.Charset
import java.net.URL

package object newman extends NewmanPrivate {
  type IOValidation[Fail, Success] = IO[Validation[Fail, Success]]
  type IOPromiseValidation[Fail, Success] = IO[Promise[Validation[Fail, Success]]]

  type Header = (String, String)
  type HeaderList = NonEmptyList[Header]
  type Headers = Option[HeaderList]

  object HeaderList {
    implicit val HeaderListShow = new Show[HeaderList] {
      override def shows(headerList: HeaderList): String = {
        headerList.list.map { header =>
          s"${header._1}=${header._2}"
        }.mkString("&")
      }
    }
  }

  object Headers {
    implicit val HeadersEqual = new Equal[Headers] {
      override def equal(headers1: Headers, headers2: Headers): Boolean = (headers1, headers2) match {
        case (Some(h1), Some(h2)) => h1.list === h2.list
        case (None, None) => true
        case _ => false
      }
    }

    implicit val HeadersMonoid: Monoid[Headers] =
      Monoid.instance((mbH1, mbH2) => (mbH1 tuple mbH2).map(h => h._1.append(h._2)), Headers.empty)

    implicit val HeadersShow = new Show[Headers] {
      import HeaderList.HeaderListShow
      override def shows(h: Headers): String = {
        val s = ~h.map { headerList: HeaderList =>
          headerList.shows
        }
        s
      }
    }

    def apply(h: Header): Headers = Headers(nels(h))
    def apply(h: Header, tail: Header*): Headers = Headers(nel(h, tail.toList))
    def apply(h: HeaderList): Headers = h.some
    def apply(h: List[Header]): Headers = h.toNel
    def empty: Option[HeaderList] = Option.empty[HeaderList]
  }

  type RawBody = Array[Byte]
  implicit val RawBodyMonoid: Monoid[RawBody] = Monoid.instance(_ ++ _, Array[Byte]())
  object RawBody {
    def apply(s: String, charset: Charset = Constants.UTF8Charset): Array[Byte] = s.getBytes(charset)
    def apply(b: Array[Byte]): Array[Byte] = b
    lazy val empty = Array[Byte]()
  }

  /**
   * a class extension for Scalaz's {{Promise}}
   * @param prom the promise that will be extended
   * @tparam T the type that the promise contains
   */
  implicit class RichPromise[T](prom: ScalazPromise[T]) {
    /**
     * convert the extended Promise to a {{scala.concurrent.Future[T]}}
     * @return the Future. will be completed when the extended promise is completed.
     */
    def toScalaFuture: ScalaFuture[T] = {
      val scalaProm = ScalaPromise[T]()
      prom.to(
        k = { result: T =>
          scalaProm.success(result)
        },
        err = { throwable: Throwable =>
          scalaProm.failure(throwable)
        }
      )
      scalaProm.future
    }

    /**
     * catch exceptions thrown inside the promise
     * @param fn the function to convert the thrown exception into the expected return type
     * @return a new promise, which will not throw
     */
    def except(fn: Throwable => T): ScalazPromise[T] = {
      //emptyPromise uses the same strategy as the extended promise
      val returnPromise = Promise.emptyPromise[T](prom.strategy)

      def onSuccess(response: T) {
        returnPromise.fulfill(response)
      }
      def onFailure(t: Throwable) {
        returnPromise.fulfill(fn(t))
      }
      prom.to(k = onSuccess, err = onFailure)
      returnPromise
    }

    /**
     * ensure that a side effect occurs when the promise finishes, regardless of its state
     * (thrown or successfully completed)
     * @param action the action to take
     * @return the same promise, which will now execute the side effect when it finishes
     */
    def ensure(action: => Unit): ScalazPromise[T] = {
      prom.to(
        k = { t: T =>
          action
        }, err = { t: Throwable =>
          action
        }
      )
      prom
    }
  }

  implicit class RichURL(url: URL) {
    def hostAndPort: (String, Int) = {
      val host = url.getHost
      val port = url.getPort match {
        case -1 => 80
        case other => other
      }
      host -> port
    }
  }
}
