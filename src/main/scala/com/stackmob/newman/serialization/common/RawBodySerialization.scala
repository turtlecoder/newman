package com.stackmob.newman.serialization.common

import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.newman.request.HttpRequestWithBody.RawBody
import com.stackmob.newman.Constants._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.serialization.common
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 4:56 PM
 */

object RawBodySerialization extends SerializationBase[RawBody] {
  implicit override val reader = new JSONR[RawBody] {
    override def read(json: JValue) = json match {
      case JString(s) => s.getBytes(UTF8Charset).successNel[Error]
      case j => UnexpectedJSONError(j, classOf[JString]).failNel[RawBody]
    }
  }

  implicit override val writer = new JSONW[RawBody] {
    override def write(r: RawBody) = JString(new String(r, UTF8Charset))
  }
}
