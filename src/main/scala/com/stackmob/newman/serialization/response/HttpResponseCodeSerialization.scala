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
package serialization.response

import scalaz._
import scalaz.Validation._
import Scalaz._
import com.stackmob.newman.response.HttpResponseCode
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import serialization.common.SerializationBase

object HttpResponseCodeSerialization extends SerializationBase[HttpResponseCode] {
  override implicit val writer = new JSONW[HttpResponseCode] {
    override def write(h: HttpResponseCode): JValue = JInt(h.code)
  }

  override implicit val reader = new JSONR[HttpResponseCode] {
    override def read(json: JValue): Result[HttpResponseCode] = {
      json match {
        case JInt(code) => fromTryCatch(HttpResponseCode.fromInt(code.toInt)).fold(
          succ = {
            o: Option[HttpResponseCode] =>
              o.map {
                c: HttpResponseCode => c.successNel[Error]
              } | {
                UncategorizedError("response code", "Unknown Http Response Code %d".format(code), List()).failNel[HttpResponseCode]
              }
          },
          fail = { t: Throwable =>
            UncategorizedError("response code", t.getMessage, List()).failNel[HttpResponseCode]
          }
        )
        case j => UnexpectedJSONError(j, classOf[JInt]).failNel[HttpResponseCode]
      }
    }
  }
}
