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

abstract class BaseHttpResponse() {

  val code: HttpResponseCode
  val headers: Headers
  val timeReceived: Date = new Date()

  import HttpResponseCode.HttpResponseCodeEqual

  def bodyString(charset: Charset = UTF8Charset): String

  lazy val etag: Option[String] = headers.flatMap { headerList: HeaderList =>
    headerList.list.find(h => h._1 === HttpHeaders.ETAG).map(h => h._2)
  }

  lazy val notModified: Boolean = code === HttpResponseCode.NotModified
}

case class HttpResponse(code: HttpResponseCode, headers: Headers, body: RawBody, override val timeReceived: Date = new Date()) extends BaseHttpResponse {
  override def bodyString(charset: Charset = UTF8Charset) = new String(body, charset)

  def toJValue: JValue = {
    import com.stackmob.newman.serialization.response.HttpResponseSerialization._
    import net.liftweb.json.scalaz.JsonScalaz.toJSON
    toJSON(this)
  }

  def toJson(prettyPrint: Boolean = false) = if(prettyPrint) {
    pretty(render(toJValue))
  } else {
    compact(render(toJValue))
  }

  //allow implicit conversion to be called directly
  def as[A <: AnyRef](implicit m:Manifest[A]) = HttpResponse.rawToTyped[A](this)

  //todo: make this actually work
  //todo: partial function on range of codes
  def apply[A <: AnyRef](pf: PartialFunction[HttpResponseCode, Class[_]])(implicit m:Manifest[A]): Result[A] = {
    def theReader(implicit reader: JSONR[A] = DefaultBodySerialization.getReader) = reader
    implicit val formats = DefaultFormats.withHints(ShortTypeHints(List(pf.apply(code))))
     this.as[A].body
  }
}

//todo: add conversions from Result to Option & Box
//maybe a more complicated conversion that returns a Validation/Box[HttpResponse], with serialization failure info
case class TypedHttpResponse[A](code: HttpResponseCode, headers: Headers, body: Result[A], override val timeReceived: Date = new Date()) extends BaseHttpResponse {
  override def bodyString(charset: Charset = UTF8Charset) = body.toString
}

object HttpResponse {
  import net.liftweb.json.scalaz.JsonScalaz.Result
  def fromJValue(jValue: JValue): Result[HttpResponse] = {
    import com.stackmob.newman.serialization.response.HttpResponseSerialization._
    import net.liftweb.json.scalaz.JsonScalaz.fromJSON
    fromJSON(jValue)
  }

  def fromJson(json: String): Result[HttpResponse] = validating({
    parse(json)
  }).mapFailure({ t: Throwable =>
    UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
  }).liftFailNel.flatMap(fromJValue(_))


   implicit def rawToTyped[A <: AnyRef](raw: HttpResponse)(implicit m:Manifest[A]): TypedHttpResponse[A] = {
     def theReader(implicit reader: JSONR[A] = DefaultBodySerialization.getReader) = reader
     TypedHttpResponse[A](raw.code, raw.headers, fromJSON[A](parse(raw.bodyString()))(theReader))
   }
}
