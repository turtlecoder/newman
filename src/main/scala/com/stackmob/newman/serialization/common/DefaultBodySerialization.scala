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

import scalaz.Validation._
import org.json4s._
import org.json4s.scalaz.JsonScalaz._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._

object DefaultBodySerialization {

  def getReader[A <: AnyRef](implicit m:Manifest[A]): JSONR[A] = new JSONR[A] {
    override def read(json: JValue): Result[A] = {
      (fromTryCatch {
        json.extract[A](Serialization.formats(NoTypeHints), m)
      } leftMap{ t: Throwable =>
        UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
      }).toValidationNel
    }
  }

  def getWriter[A <: AnyRef]: JSONW[A] = new JSONW[A] {
    override def write(obj: A): JValue = {
      parse(Serialization.write(obj)(Serialization.formats(NoTypeHints)))
    }
  }

}
