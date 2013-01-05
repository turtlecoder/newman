/**
 * Copyright 2013 StackMob
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

package com.stackmob.newman.request

import com.stackmob.common.enumeration._
import scalaz._
import Scalaz._

sealed abstract class HttpRequestType(override val stringVal: String) extends Enumeration
object HttpRequestType {
  object GET extends HttpRequestType("GET")
  object POST extends HttpRequestType("POST")
  object PUT extends HttpRequestType("PUT")
  object DELETE extends HttpRequestType("DELETE")
  object HEAD extends HttpRequestType("HEAD")

  implicit val HttpRequestTypeToReader = new EnumReader[HttpRequestType] {
    override def read(s: String) = s.toUpperCase match {
      case GET.stringVal => GET.some
      case POST.stringVal => POST.some
      case PUT.stringVal => PUT.some
      case DELETE.stringVal => DELETE.some
      case HEAD.stringVal => HEAD.some
      case _ => none
    }
  }
}
