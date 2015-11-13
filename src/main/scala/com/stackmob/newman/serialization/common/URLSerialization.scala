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

package com.stackmob.newman.serialization.common

import scalaz._
import Scalaz._
import org.json4s._
import org.json4s.scalaz.JsonScalaz._
import java.net.URL

object URLSerialization extends SerializationBase[URL] {
  implicit override val writer = new JSONW[URL] {
    override def write(u: URL): JValue = JString(u.toString)
  }

  implicit override val reader = new JSONR[URL] {
    override def read(jValue: JValue): Result[URL] = jValue match {
      case JString(s) => new URL(s).successNel[Error]
      case j => UnexpectedJSONError(j, classOf[JString]).failNel[URL]
    }
  }
}
