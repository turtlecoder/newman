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

package com.stackmob.newman
package response

import scalaz._
import Scalaz._
import scalaz.NonEmptyList._
import jsonscalaz._
import java.nio.charset.Charset
import java.util.Date
import com.stackmob.newman.Constants._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import org.apache.http.HttpHeaders
import com.stackmob.newman.serialization.response.HttpResponseSerialization
import com.stackmob.newman.serialization.common.DefaultBodySerialization
import java.util.concurrent.ConcurrentHashMap
import scala.collection.convert.Wrappers.JConcurrentMapWrapper

case class HttpResponse(code: HttpResponseCode,
                        headers: Headers,
                        rawBody: RawBody,
                        timeReceived: Date = new Date()) {
  import HttpResponse._

  private lazy val rawBodyMap = new JConcurrentMapWrapper(new ConcurrentHashMap[Charset, String])
  private lazy val parsedBodyMap = new JConcurrentMapWrapper(new ConcurrentHashMap[(Charset, Class[_]), Result[_]])
  private lazy val jValueMap = new JConcurrentMapWrapper(new ConcurrentHashMap[Charset, JValue])
  private lazy val jsonMap = new JConcurrentMapWrapper(new ConcurrentHashMap[(Charset, Boolean), String])
  private lazy val caseClassMap = new JConcurrentMapWrapper(new ConcurrentHashMap[(Charset, Class[_]), Result[_]])

  def bodyString(implicit charset: Charset = defaultCharset): String = {
    rawBodyMap.getOrElseUpdate(charset, new String(rawBody, charset))
  }

  def toJValue(implicit charset: Charset = defaultCharset): JValue = {
    jValueMap.getOrElseUpdate(charset, toJSON(this)(getResponseSerialization.writer))
  }

  def toJson(prettyPrint: Boolean = false)(implicit charset: Charset = defaultCharset): String = {
    jsonMap.getOrElseUpdate((charset, prettyPrint), {
      if(prettyPrint) {
        pretty(render(toJValue))
      } else {
        compactRender(toJValue)
      }
    })
  }

  def bodyAsCaseClass[T <: AnyRef](implicit m: Manifest[T], charset: Charset = defaultCharset): Result[T] = {
    def theReader(implicit reader: JSONR[T] = DefaultBodySerialization.getReader): JSONR[T] = reader
    caseClassMap.getOrElseUpdate((charset, m.runtimeClass), {
      fromJSON[T](parse(bodyString(charset)))(theReader)
    }).map(_.asInstanceOf[T])
  }

  def bodyAs[T](implicit reader: JSONR[T],
                m: Manifest[T],
                charset: Charset = defaultCharset): Result[T] = {
    parsedBodyMap.get((charset, m.runtimeClass)) match {
      case Some(v) => v.map(_.asInstanceOf[T])
      case None => {
        val d = parseBody[T]
        parsedBodyMap((charset, m.runtimeClass)) = d
        d
      }
    }
  }

  private def parseBody[T](implicit reader: JSONR[T],
                           charset: Charset = defaultCharset): Result[T] = {
    Validation.fromTryCatch {
      parse(bodyString(charset))
    } leftMap { t: Throwable =>
      nels(UncategorizedError(t.getClass.getCanonicalName, t.getMessage, Nil))
    } flatMap { jValue: JValue =>
      fromJSON[T](jValue)
    }
  }

  def bodyAsIfResponseCode[T](expected: HttpResponseCode,
                              decoder: HttpResponse => ThrowableValidation[T]): ThrowableValidation[T] = {
    for {
      _ <- {
        if(this.code === expected) {
          ().success[Throwable]
        } else {
          (UnexpectedResponseCode(expected, this.code): Throwable).fail[T]
        }
      }
      body <- {
        try {
          decoder(this)
        } catch {
          case t: Throwable => t.fail[T]
        }
      }
    } yield {
      body
    }
  }

  def bodyAsIfResponseCode[T](expected: HttpResponseCode)
                             (implicit reader: JSONR[T],
                              m: Manifest[T],
                              charset: Charset = defaultCharset): ThrowableValidation[T] = {
    bodyAsIfResponseCode[T](expected, { resp: HttpResponse =>
      bodyAs[T].leftMap { errNel: NonEmptyList[Error] =>
        val t: Throwable = JSONParsingError(errNel)
        t
      }
    })
  }

  lazy val eTag: Option[String] = headers.flatMap { headerList: HeaderList =>
    headerList.list.find(h => h._1 === HttpHeaders.ETAG).map(h => h._2)
  }

  lazy val notModified: Boolean = code === HttpResponseCode.NotModified

  lazy val responseCharset: Option[Charset] = headers.flatMap(headersList => {
    headersList.list.collectFirst({
      case (HttpHeaders.CONTENT_TYPE, CHARSET_PATTERN(_, charset)) =>
        charset
    })
  }).flatMap(charset => {
    try {
      Some(Charset.forName(charset))
    } catch {
      case _: Throwable => None
    }
  })

  private def defaultCharset = responseCharset.getOrElse(UTF8Charset)
}

object HttpResponse {
  private def getResponseSerialization(implicit charset: Charset = UTF8Charset) = new HttpResponseSerialization(charset)
  def fromJValue(jValue: JValue)(implicit charset: Charset = UTF8Charset): Result[HttpResponse] = {
    fromJSON(jValue)(getResponseSerialization.reader)
  }

  def fromJson(json: String): Result[HttpResponse] = (Validation.fromTryCatch {
    parse(json)
  } leftMap { t: Throwable =>
    UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
  }).toValidationNel.flatMap { j: JValue => fromJValue(j) }

  case class UnexpectedResponseCode(expected: HttpResponseCode, actual: HttpResponseCode)
    extends Exception("expected response code %d, got %d".format(expected.code, actual.code))

  private def getString(errNel: NonEmptyList[Error]): String = {
    errNel.map { err: Error =>
      err.fold(
        u => "unexpected JSON %s. expected %s".format(u.was.toString, u.expected.getCanonicalName),
        n => "no such field %s in json %s".format(n.name, n.json.toString),
        u => "uncategorized error %s while trying to decode JSON: %s".format(u.key, u.desc)
      )
    }.list.mkString("\n")
  }

  case class JSONParsingError(errNel: NonEmptyList[Error]) extends Exception(getString(errNel))

  private val CHARSET_PATTERN = """(.+);\s+charset=(.+)""".r
}
