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
package serialization.common

import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._

object HeadersSerialization extends SerializationBase[Headers] {

  implicit override val writer = new JSONW[Headers] {
    override def write(h: Headers): JValue = {
      val headersList: List[JObject] = h.map {
        headerList: HeaderList =>
          (headerList.list.map { header: Header =>
              JObject(JField("name", JString(header._1)) :: JField("value", JString(header._2)) :: Nil)
          }).toList
      } | List[JObject]()

      JArray(headersList)
    }
  }

  implicit override val reader = new JSONR[Headers] {
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
          val list = jObjectList.flatMap {
            jValue: JValue =>
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
