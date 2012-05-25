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

trait BaseHttpResponse {

  def code: HttpResponseCode
  def headers: Headers
  def timeReceived: Date = new Date()

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
  def as[A <: AnyRef : Manifest] = HttpResponse.rawToTyped[A](this)
}

//todo: add conversions from Result to Option & Box
//maybe a more complicated conversion that returns a Validation/Box[HttpResponse], with//return string of errors serialization failure info
case class TypedHttpResponse[A](code: HttpResponseCode, headers: Headers, body: Result[A], override val timeReceived: Date = new Date()) extends BaseHttpResponse {
  override def bodyString(charset: Charset = UTF8Charset) =
    body.map(_.toString) |||
      { errors: NonEmptyList[Error] => errors.map( _ match {
        case UnexpectedJSONError(was, expected) => "UnexpectedJSONError: expected " + expected + ", was " + was
        case NoSuchFieldError(name, json) => "NoSuchFieldError: no such field " + name + " in " + json
        case UncategorizedError(key, desc, _) =>  "UncategorizedError: " + key + ", " + desc
      } ).list.mkString("Errors: ", ",", "")     }
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


   implicit def rawToTyped[A <: AnyRef: Manifest](raw: HttpResponse): TypedHttpResponse[A] = {
     def theReader(implicit reader: JSONR[A] = DefaultBodySerialization.getReader) = reader
     TypedHttpResponse[A](raw.code, raw.headers, fromJSON[A](parse(raw.bodyString()))(theReader), raw.timeReceived)
   }
}
