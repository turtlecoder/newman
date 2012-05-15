package com.stackmob.newman.serialization.common

import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import java.net.URL

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.serialization.common
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 4:16 PM
 */

object URLSerialization extends SerializationBase[URL] {
  implicit override val writer = new JSONW[URL] {
    override def write(u: URL) = JString(u.toString)
  }

  implicit override val reader = new JSONR[URL] {
    override def read(jValue: JValue) = jValue match {
      case JString(s) => new URL(s).successNel[Error]
      case j => UnexpectedJSONError(j, classOf[JString]).failNel[URL]
    }
  }
}
