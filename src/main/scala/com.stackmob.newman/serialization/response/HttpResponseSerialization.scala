package com.stackmob.newman.serialization.response

import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.newman.request.HttpRequest._
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import java.util.Date
import com.stackmob.newman.Constants._
import com.stackmob.newman.serialization.common._
import java.nio.charset.Charset

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.serialization.response
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 1:36 PM
 */


//TODO: make this a class that takes in a charset: Charset = UTF8Charset param,
//so that when converting to/from string we have a valid charset
class HttpResponseSerialization(charset: Charset = UTF8Charset) extends SerializationBase[HttpResponse] {
  protected val CodeKey = "code"
  protected val HeadersKey = "headers"
  protected val BodyKey = "body"
  protected val TimeReceivedKey = "time_received"

  override implicit val writer = new JSONW[HttpResponse] {

    import HeadersSerialization.{writer => HeadersWriter}
    import HttpResponseCodeSerialization.{writer => ResponseCodeWriter}

    override def write(h: HttpResponse): JValue = {
      JObject(
        JField(CodeKey, toJSON(h.code)(ResponseCodeWriter)) ::
        JField(HeadersKey, toJSON(h.headers)(HeadersWriter)) ::
        JField(BodyKey, JString(h.bodyString(charset))) ::
        JField(TimeReceivedKey, JInt(h.timeReceived.getTime)) ::
        Nil
      )
    }
  }

  override implicit val reader = new JSONR[HttpResponse] {

    import HeadersSerialization.{reader => HeadersReader}
    import HttpResponseCodeSerialization.{reader => ResponseCodeReader}

    override def read(json: JValue): Result[HttpResponse] = {
      val codeField = field[HttpResponseCode](CodeKey)(json)(ResponseCodeReader)
      val headersField = field[Headers](HeadersKey)(json)(HeadersReader)
      val bodyField = field[String](BodyKey)(json)
      val timeReceivedField = field[Long](TimeReceivedKey)(json)

      (codeField |@| headersField |@| bodyField |@| timeReceivedField) {
        (code: HttpResponseCode, headers: Headers, body: String, timeReceivedMilliseconds: Long) =>
          HttpResponse(code, headers, body.getBytes(charset), new Date(timeReceivedMilliseconds))
      }
    }
  }
}
