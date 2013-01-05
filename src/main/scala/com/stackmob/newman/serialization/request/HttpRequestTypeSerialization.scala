package com.stackmob.newman
package serialization.request

import scalaz._
import Scalaz._
import enumeration._
import com.stackmob.newman.request.HttpRequestType
import com.stackmob.newman.serialization.common.SerializationBase
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.serialization.request
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 3:56 PM
 */

object HttpRequestTypeSerialization extends SerializationBase[HttpRequestType] {
  implicit override val reader = new JSONR[HttpRequestType] {
    override def read(jValue: JValue): ValidationNEL[Error, HttpRequestType] = jValue match {
      case JString(s) => s.readEnum[HttpRequestType].map(_.successNel[Error]) | {
        UncategorizedError("request type", "unknown request type %s".format(s), List()).failNel[HttpRequestType]
      }
      case _ => NoSuchFieldError("request type", jValue).failNel[HttpRequestType]
    }
  }

  implicit override val writer = new JSONW[HttpRequestType] {
    override def write(t: HttpRequestType): JValue = JString(t.stringVal)
  }
}
