package com.stackmob.newman.response.serialization

import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.common.validation._
import com.stackmob.newman.request.HttpRequest._
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import java.util.Date
import com.stackmob.newman.Constants._


/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.response.caching
 *
 * User: aaron
 * Date: 5/2/12
 * Time: 11:55 PM
 */

trait CommonSerialization {
  protected val CodeKey = "code"
  protected val HeadersKey = "headers"
  protected val BodyKey = "body"
  protected val TimeReceivedKey = "time_received"
}

object HttpResponseCodeSerialization extends CommonSerialization {
  implicit def HttpResponseCodeJSONW: JSONW[HttpResponseCode] = new JSONW[HttpResponseCode] {
    override def write(h: HttpResponseCode): JValue = JInt(h.code)
  }

  implicit def HttpResponseCodeJSONR: JSONR[HttpResponseCode] = new JSONR[HttpResponseCode] {
    override def read(json: JValue): Result[HttpResponseCode] = {
      json match {
        case JInt(code) => validating(HttpResponseCode.fromInt(code.toInt)).fold(
          success = {
            o: Option[HttpResponseCode] =>
              o.map {
                c: HttpResponseCode => c.successNel[Error]
              } | {
                UncategorizedError(CodeKey, "Unknown Http Response Code %d".format(code), List()).failNel[HttpResponseCode]
              }
          },
          failure = {
            t: Throwable =>
              UncategorizedError(CodeKey, t.getMessage, List()).failNel[HttpResponseCode]
          }
        )
        case j => UnexpectedJSONError(j, classOf[JInt]).failNel[HttpResponseCode]
      }
    }
  }
}

object HeadersSerialization extends CommonSerialization {

  implicit def HeadersJSONW: JSONW[Headers] = new JSONW[Headers] {
    override def write(h: Headers): JValue = {
      val headersList: List[JObject] = h.map {
        headerList: HeaderList =>
          (headerList.list.map {
            header: Header =>
              JObject(JField("name", JString(header._1)) :: JField("value", JString(header._2)) :: Nil)
          }).toList
      } | List[JObject]()

      JArray(headersList)
    }
  }

  implicit def HeadersJSONR: JSONR[Headers] = new JSONR[Headers] {
    override def read(json: JValue): Result[Headers] = {
      //example incoming AST:
      //JArray(
      //  List(
      //    JObject(
      //      List(
      //        JField(name,JString(header1)),
      //        JField(value,JString(header1))
      //      )
      //    )
      //  )
      //)
      json match {
        case JArray(jObjectList) => {
          val list = jObjectList.flatMap { jValue: JValue =>
            jValue match {
              case JObject(jFieldList) => jFieldList match {
                case JField(_, JString(headerName)) :: JField(_, JString(headerVal)) :: Nil => List(headerName -> headerVal)
                //TODO: error here
                case _ => List[(String, String)]()
              }
              //TODO: error here
              case _ => List[(String, String)]()
            }
          }
          val headers: Headers = Headers(list)
          headers.successNel[Error]
        }
        case j => UnexpectedJSONError(j, classOf[JArray]).failNel[Headers]
      }
    }
  }
}

object HttpResponseSerialization extends CommonSerialization {
  implicit def HttpResponseJSONW: JSONW[HttpResponse] = new JSONW[HttpResponse] {

    import HttpResponseCodeSerialization.HttpResponseCodeJSONW
    import HeadersSerialization.HeadersJSONW

    override def write(h: HttpResponse): JValue = {
      val bodyString = new String(h.body, UTF8Charset)
      JObject(
        JField(CodeKey, toJSON(h.code)) ::
        JField(HeadersKey, toJSON(h.headers)) ::
        JField(BodyKey, JString(bodyString)) ::
        JField(TimeReceivedKey, JInt(h.timeReceived.getTime)) ::
        Nil
      )
    }
  }

  implicit def HttpResponseJSONR: JSONR[HttpResponse] = new JSONR[HttpResponse] {

    import HeadersSerialization.HeadersJSONR
    import HttpResponseCodeSerialization.HttpResponseCodeJSONR

    override def read(json: JValue): Result[HttpResponse] = {
      val codeField = field[HttpResponseCode](CodeKey)(json)(HttpResponseCodeJSONR)
      val headersField = field[Headers](HeadersKey)(json)(HeadersJSONR)
      val bodyField = field[String](BodyKey)(json)
      val timeReceivedField = field[Long](TimeReceivedKey)(json)

      (codeField |@| headersField |@| bodyField |@| timeReceivedField) {
        (code: HttpResponseCode, headers: Headers, body: String, timeReceivedMilliseconds: Long) =>
        HttpResponse(code, headers, body.getBytes(UTF8Charset), new Date(timeReceivedMilliseconds))
      }
    }
  }
}
