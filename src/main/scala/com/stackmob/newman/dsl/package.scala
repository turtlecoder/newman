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

import request.HttpRequest
import response.HttpResponse
import scalaz._
import scalaz.effect.IO
import Scalaz._
import java.net.URL

package object dsl extends URLBuilderDSL with RequestBuilderDSL with ResponseHandlerDSL {

  sealed trait Protocol {
    def name: String
  }

  case object http extends Protocol {
    override lazy val name = "http"
  }

  case object https extends Protocol {
    override lazy val name = "https"
  }

  implicit def urlCapableToURL(c: URLCapable): URL = c.toURL
  implicit def stringToPath(s: String): Path = Path(s :: Nil)

  implicit def transformerToHttpRequest(t: Builder): HttpRequest = t.toRequest

  implicit def ioRespToW(ioResp: IO[HttpResponse]): IOResponseW = new IOResponseW {
    override val value = ioResp
  }

  implicit def ResponseHandlerToResponse[Failure, Success](handler: ResponseHandler[Failure, Success]): IOValidation[Failure, Success] = {
    handler.toIO
  }

}
