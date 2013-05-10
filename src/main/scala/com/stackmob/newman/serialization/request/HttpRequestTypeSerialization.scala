/**
 * Copyright 2012-2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.newman
package serialization.request

import scalaz._
import Scalaz._
import enumeration._
import com.stackmob.newman.request.HttpRequestType
import com.stackmob.newman.serialization.common.SerializationBase
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._

object HttpRequestTypeSerialization extends SerializationBase[HttpRequestType] {
  implicit override val reader = new JSONR[HttpRequestType] {
    override def read(jValue: JValue): ValidationNel[Error, HttpRequestType] = jValue match {
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
