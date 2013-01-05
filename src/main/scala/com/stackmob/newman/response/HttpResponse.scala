package com.stackmob.newman
package response

import scalaz._
import effects._
import Scalaz._
import request._
import jsonscalaz._
import HttpRequest._
import HttpRequestWithBody._
import java.nio.charset.Charset
import java.util.Date
import com.stackmob.newman.Constants._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import org.apache.http.HttpHeaders
import com.stackmob.newman.response.HttpResponseCode.HttpResponseCodeEqual
import com.stackmob.newman.serialization.response.HttpResponseSerialization
import com.stackmob.newman.serialization.common.DefaultBodySerialization

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.response
 *
 * User: aaron
 * Date: 5/2/12
 * Time: 11:54 PM
 */

case class HttpResponse(code: HttpResponseCode,
                        headers: Headers,
                        rawBody: RawBody,
                        timeReceived: Date = new Date()) {
  import HttpResponse._

  def bodyString(implicit charset: Charset = UTF8Charset) = new String(rawBody, charset)

  def toJValue(implicit charset: Charset = UTF8Charset): JValue = toJSON(this)(getResponseSerialization.writer)

  def toJson(prettyPrint: Boolean = false): String = if(prettyPrint) {
    pretty(render(toJValue))
  } else {
    compact(render(toJValue))
  }

  def bodyAsCaseClass[T <: AnyRef](implicit m: Manifest[T], charset: Charset = UTF8Charset) = {
    def theReader(implicit reader: JSONR[T] = DefaultBodySerialization.getReader) = reader
    fromJSON[T](parse(bodyString(charset)))(theReader)
  }

  def bodyAs[T](implicit reader: JSONR[T],
                charset: Charset = UTF8Charset): Result[T] = validating {
    parse(bodyString(charset))
  } mapFailure { t: Throwable =>
    nel(UncategorizedError(t.getClass.getCanonicalName, t.getMessage, Nil))
  } flatMap { jValue: JValue =>
    fromJSON[T](jValue)
  }

  def bodyAsIfResponseCode[T](expected: HttpResponseCode,
                              decoder: HttpResponse => ThrowableValidation[T]): ThrowableValidation[T] = {
    val valT = for {
      resp <- validationT[IO, Throwable, HttpResponse](validating(this).pure[IO])
      _ <- validationT[IO, Throwable, Unit] {
        if(resp.code === expected) {
          ().success[Throwable].pure[IO]
        } else {
          (UnexpectedResponseCode(expected, resp.code): Throwable).fail[Unit].pure[IO]
        }
      }
      body <- validationT[IO, Throwable, T] {
        io(decoder(resp)).except(t => t.fail[T].pure[IO])
      }
    } yield body
    valT.run.unsafePerformIO
  }

  def bodyAsIfResponseCode[T](expected: HttpResponseCode)
                             (implicit reader: JSONR[T],
                              charset: Charset = UTF8Charset): ThrowableValidation[T] = {
    bodyAsIfResponseCode[T](expected, { resp: HttpResponse =>
      bodyAs[T].mapFailure { errNel: NonEmptyList[Error] =>
        val t: Throwable = JSONParsingError(errNel)
        t
      }
    })
  }

  lazy val eTag: Option[String] = headers.flatMap { headerList: HeaderList =>
    headerList.list.find(h => h._1 === HttpHeaders.ETAG).map(h => h._2)
  }

  lazy val notModified: Boolean = code === HttpResponseCode.NotModified
}

object HttpResponse {
  private def getResponseSerialization(implicit charset: Charset = UTF8Charset) = new HttpResponseSerialization(charset)
  def fromJValue(jValue: JValue)(implicit charset: Charset = UTF8Charset): Result[HttpResponse] = {
    fromJSON(jValue)(getResponseSerialization.reader)
  }

  def fromJson(json: String): Result[HttpResponse] = (validating {
    parse(json)
  } mapFailure { t: Throwable =>
    UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
  }).liftFailNel.flatMap(fromJValue(_))

  case class UnexpectedResponseCode(expected: HttpResponseCode, actual: HttpResponseCode)
    extends Exception("expected response code %d, got %d".format(expected.code, actual.code))

  case class JSONParsingError(errNel: NonEmptyList[Error]) extends Exception({
    errNel.map { err: Error =>
      err.fold(
        u => "unexpected JSON %s. expected %s".format(u.was.toString, u.expected.getCanonicalName),
        n => "no such field %s in json %s".format(n.name, n.json.toString),
        u => "uncategorized error %s while trying to decode JSON: %s".format(u.key, u.desc)
      )
    }.list.mkString("\n")
  })
}
