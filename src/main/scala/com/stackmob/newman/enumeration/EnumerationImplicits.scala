package com.stackmob.newman
package enumeration

import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._

/**
 * Created by IntelliJ IDEA.
 * 
 * com.stackmob.newman.enumeration
 * 
 * User: aaron
 * Date: 12/27/12
 * Time: 10:10 PM
 */

trait EnumerationImplicits {
  implicit def stringToStringEnumW(s: String) = new StringEnumReaderW {
    def value: String = s
  }

  implicit def enumerationJSON[T <: Enumeration](implicit reader: EnumReader[T], m: Manifest[T]) = new JSON[T] {
    override def write(value: T): JValue = JString(value.stringVal)
    override def read(json: JValue): Result[T] = json match {
      case JString(s) => (validating(reader.withName(s)).mapFailure { _ =>
        UncategorizedError(s, "Invalid %s: %s".format(m.erasure.getSimpleName, s), Nil)
      }).liftFailNel
      case j => UnexpectedJSONError(j, classOf[JString]).failNel
    }
  }

}