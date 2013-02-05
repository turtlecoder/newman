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

package com.stackmob.newman.dsl

import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import scalaz._
import effects._
import Scalaz._
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}

class ResponseHandlerDSLSpecs extends Specification { def is =
  "ResponseHandlerDSLSpecs".title                                                                                       ^
  """
  ResponseHandlerDSL is a DSL used to handle responses with Newman
  """                                                                                                                   ^
  "The DSL should"                                                                                                      ^
    "return a Failure if the IO throws"                                                                                 ! ThrowingIO().returnsFailure ^
    "return non throwable error types if specified"                                                                     ! CustomErrors().returnsCorrectly ^
                                                                                                                        end
  trait Context

  import com.stackmob.newman.dsl._

  case class ThrowingIO() extends Context {
    def returnsFailure: SpecsResult = {
      val ex = new Exception("test exception")
      val respIO = io((throw ex): HttpResponse).handleCode(HttpResponseCode.Ok)(_ => ().success)
      respIO.unsafePerformIO.either must beLeft.like {
        case e => e must beEqualTo(ex)
      }
    }
  }

  case class CustomErrors() extends CustomErrorContext {
    def returnsCorrectly: SpecsResult = {
      val ex = new Exception("test exception")
      val customError = new CustomErrorForSpecs("test exception")
      val respIO: IO[Validation[CustomErrorForSpecs, Unit]] = io((throw ex): HttpResponse).handleCode[CustomErrorForSpecs, Unit](HttpResponseCode.Ok)(_ => ().success)
      respIO.unsafePerformIO.either must beLeft.like {
        case e => e must beEqualTo(customError)
      }
    }
  }

  trait CustomErrorContext extends Context {
    case class CustomErrorForSpecs(msg: String)

    implicit def exToCustomError(ex: Throwable): CustomErrorForSpecs = new CustomErrorForSpecs(ex.getMessage())
  }

}
