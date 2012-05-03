package com.stackmob.newman.response.caching

import java.nio.charset.Charset
import scalaz._
import Scalaz._
import net.liftweb.json.scalaz.JsonScalaz._
import net.liftweb.json._
import net.liftweb.json.{NoTypeHints, Serialization}
import com.stackmob.common.validation._
import com.stackmob.newman.HttpClient.{Header, HeaderList, Headers}
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}


/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.response.caching
 *
 * User: aaron
 * Date: 5/2/12
 * Time: 11:55 PM
 */

trait CommonCaching {
  protected val CodeKey = "code"
  protected val HeadersKey = "headers"
  protected val BodyKey = "body"
  protected val UTF8Charset = Charset.forName("UTF-8")
}

object HttpResponseCodeCaching extends CommonCaching {
  implicit def HttpResponseCodeJSONW: JSONW[HttpResponseCode] = new JSONW[HttpResponseCode] {
    override def write(h: HttpResponseCode): JValue = JField(CodeKey, JInt(h.code))
  }

  implicit def HttpResponseCodeJSONR: JSONR[HttpResponseCode] = new JSONR[HttpResponseCode] {
    override def read(jValue: JValue): Result[HttpResponseCode] = {
      (jValue \ CodeKey) match {
        case JInt(code) => validating(HttpResponseCode.fromInt(code.toInt)).fold(
          success = { o: Option[HttpResponseCode] =>
            o.map { c: HttpResponseCode => c.successNel[Error]} | {
              UncategorizedError(CodeKey, "Unknown Http Response Code %d".format(code), List()).failNel[HttpResponseCode]
            }
          },
          failure = { t: Throwable =>
            UncategorizedError(CodeKey, t.getMessage, List()).failNel[HttpResponseCode]
          }
        )
        case j => UnexpectedJSONError(j, classOf[JInt]).failNel[HttpResponseCode]
      }
    }
  }
}

object HeadersCaching extends CommonCaching {
  implicit def HeadersJSONW: JSONW[Headers] = new JSONW[Headers] {
    override def write(h: Headers): JValue = {
      val headersList: List[JObject] = h.map { headerList: HeaderList =>
        (headerList.list.map { header: Header =>
          JObject(JField("name", JString(header._1)) :: JField("value", JString(header._2)) :: Nil)
        }).toList
      } | List[JObject]()

      JField(HeadersKey, JArray(headersList))
    }
  }

  implicit def HeadersJSONR: JSONR[Headers] = new JSONR[Headers] {
    override def read(json: JValue): Result[Headers] = {
      //TODO: implement
    }
  }
}

object HttpResponseCaching extends CommonCaching {
  implicit def HttpResponseJSONW: JSONW[HttpResponse] = new JSONW[HttpResponse] {
    override def write(h: HttpResponse): JValue = {
      val codeInt = h.code.code
      val headerList: List[JObject] = h.headers.map { headerList: HeaderList =>
        (headerList.list.map { header: Header =>
          JObject(JField("name", JString(header._1)) :: JField("value", JString(header._2)) :: Nil)
        }).toList
      } | List[JObject]()
      val bodyString = new String(h.body, UTF8Charset)
      JObject(
        JField(CodeKey, JInt(codeInt)) ::
        JField(HeadersKey, JArray(headerList)) ::
        JField(BodyKey, JString(bodyString)) ::
        Nil
      )
    }
  }

  implicit def HttpResponseJSONR: JSONR[HttpResponse] = new JSONR[HttpResponse] {
    import HttpResponseCodeCaching.HttpResponseCodeJSONR
    import HeadersCaching.HeadersJSONR
    override def read(json: JValue): Result[HttpResponse] =
      (field[HttpResponseCode](CodeKey)(json) |@| field[Headers](HeadersKey)(json) |@| field[String](BodyKey)(json)) {
        (code: HttpResponseCode, headers: Headers, body: String) => HttpResponse(code, headers, body.getBytes(UTF8Charset))
      }
  }

  def toJValue(h: HttpResponse): JValue = toJSON(h)
  def fromJValue(j: JValue): Result[HttpResponse] = fromJSON(j)
}
