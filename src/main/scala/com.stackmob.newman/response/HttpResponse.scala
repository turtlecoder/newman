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

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.response
 *
 * User: aaron
 * Date: 5/2/12
 * Time: 11:54 PM
 */

case class HttpResponse(code: HttpResponseCode, headers: Headers, body: RawBody, timeReceived: Date = new Date()) {
  def bodyString(charset: Charset = UTF8Charset) = new String(body, charset)

  def toJValue: JValue = {
    import net.liftweb.json.scalaz.JsonScalaz.toJSON
    import com.stackmob.newman.serialization.response.HttpResponseSerialization.writer
    toJSON(this)
  }

  def toJson(prettyPrint: Boolean = false) = if(prettyPrint) {
    pretty(render(toJValue))
  } else {
    compact(render(toJValue))
  }

  def getEtag: Option[String] = headers.flatMap { headerList: HeaderList =>
    headerList.list.find(h => h._1 === HttpHeaders.ETAG).map(h => h._2)
  }
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
}
