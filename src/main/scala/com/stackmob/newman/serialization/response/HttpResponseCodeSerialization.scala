package com.stackmob.newman.serialization.response

import scalaz._
import Scalaz._
import com.stackmob.newman.response.HttpResponseCode
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.common.validation._
import com.stackmob.newman.serialization.common.SerializationBase

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.serialization.response
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 1:44 PM
 */

object HttpResponseCodeSerialization extends SerializationBase[HttpResponseCode] {
  override implicit val writer = new JSONW[HttpResponseCode] {
    override def write(h: HttpResponseCode): JValue = JInt(h.code)
  }

  override implicit val reader = new JSONR[HttpResponseCode] {
    override def read(json: JValue): Result[HttpResponseCode] = {
      json match {
        case JInt(code) => validating(HttpResponseCode.fromInt(code.toInt)).fold(
          success = {
            o: Option[HttpResponseCode] =>
              o.map {
                c: HttpResponseCode => c.successNel[Error]
              } | {
                UncategorizedError("response code", "Unknown Http Response Code %d".format(code), List()).failNel[HttpResponseCode]
              }
          },
          failure = { t: Throwable =>
            UncategorizedError("response code", t.getMessage, List()).failNel[HttpResponseCode]
          }
        )
        case j => UnexpectedJSONError(j, classOf[JInt]).failNel[HttpResponseCode]
      }
    }
  }
}
