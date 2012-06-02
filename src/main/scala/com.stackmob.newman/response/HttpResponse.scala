package com.stackmob.newman.response

import scalaz._
import Scalaz._
import com.stackmob.newman.request._
import HttpRequest._
import HttpRequestWithBody._
import java.nio.charset.Charset
import java.util.Date
import com.stackmob.newman.Constants._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.common.validation._
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

  def toJson(prettyPrint: Boolean = false) = if(prettyPrint) {
    pretty(render(toJValue))
  } else {
    compact(render(toJValue))
  }

  def bodyAsCaseClass[T <: AnyRef](implicit m: Manifest[T], charset: Charset = UTF8Charset) = {
    def theReader(implicit reader: JSONR[T] = DefaultBodySerialization.getReader) = reader
    fromJSON[T](parse(bodyString(charset)))(theReader)
  }

  def bodyAs[T](implicit reader: JSONR[T],
                charset: Charset = UTF8Charset) = fromJSON[T](parse(bodyString(charset)))

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

  def fromJson(json: String): Result[HttpResponse] = validating({
    parse(json)
  }).mapFailure({ t: Throwable =>
    UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
  }).liftFailNel.flatMap(fromJValue(_))
}
