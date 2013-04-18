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

package com.stackmob.newman.test.dsl

import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import scalaz._
import effect.IO
import Scalaz._
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import com.stackmob.newman.{RawBody, Headers}
import com.stackmob.newman.Constants._
import language.implicitConversions

class ResponseHandlerDSLSpecs extends Specification { def is =
  "ResponseHandlerDSLSpecs".title                                                                                       ^
  """
  ResponseHandlerDSL is a DSL used to handle responses with Newman
  """                                                                                                                   ^
  "The DSL should"                                                                                                      ^
    "return a Failure if the IO throws"                                                                                 ! ThrowingIO().returnsFailure ^
    "return a Success if the IO doesn't throw, types aren't specified, and Unit is returned"                            ! ThrowingIO().returnsEmptySuccess ^
    "return a Success if the IO doesn't throw, types aren't specified, and non-Unit is returned"                        ! ThrowingIO().returnsNonEmptySuccess ^
    "return non throwable error types if specified & the IO throws"                                                     ! CustomErrors().returnsErrorCorrectly ^
    "return successful validation of nonthrowable error type if specified"                                              ! CustomErrors().returnsSuccessCorrectly ^
                                                                                                                        end
  trait Context

  import com.stackmob.newman.dsl._

  case class ThrowingIO() extends Context {
    def returnsFailure: SpecsResult = {
      val ex = new Exception("test exception")
      val respIO = IO((throw ex): HttpResponse).handleCode(HttpResponseCode.Ok)(_ => ().success)
      respIO.unsafePerformIO().toEither must beLeft.like {
        case e => e must beEqualTo(ex)
      }
    }

    def returnsEmptySuccess: SpecsResult = {
      val respIO = IO(HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)).handleCode(HttpResponseCode.Ok)(_ => ().success)
      respIO.unsafePerformIO().toEither must beRight.like {
        case e => e must beEqualTo(())
      }
    }

    def returnsNonEmptySuccess: SpecsResult = {
      val bodyString = "test body"
      val respIO = IO(HttpResponse(HttpResponseCode.Ok, Headers.empty, bodyString.getBytes(UTF8Charset))).handleCode(HttpResponseCode.Ok){resp => resp.bodyString.success}
      respIO.unsafePerformIO().toEither must beRight.like {
        case e => e must beEqualTo(bodyString)
      }
    }
  }

  case class CustomErrors() extends CustomErrorContext {
    def returnsErrorCorrectly: SpecsResult = {
      val exceptionMessage = "test exception"
      val ex = new Exception(exceptionMessage)
      val customError = new CustomErrorForSpecs(exceptionMessage)
      val respIO: IO[Validation[CustomErrorForSpecs, Unit]] = IO((throw ex): HttpResponse).handleCode[CustomErrorForSpecs, Unit](HttpResponseCode.Ok)(_ => ().success)
      respIO.unsafePerformIO().toEither must beLeft.like {
        case e => e must beEqualTo(customError)
      }
    }

    def returnsSuccessCorrectly: SpecsResult = {
      val bodyString = "test body"
      val respIO: IO[Validation[CustomErrorForSpecs, String]] = IO(HttpResponse(HttpResponseCode.Ok, Headers.empty, bodyString.getBytes(UTF8Charset))).handleCode[CustomErrorForSpecs, String](HttpResponseCode.Ok){resp => resp.bodyString.success}
      respIO.unsafePerformIO().toEither must beRight.like {
        case e => e must beEqualTo(bodyString)
      }
    }
  }

  trait CustomErrorContext extends Context {
    case class CustomErrorForSpecs(msg: String)

    implicit def exToCustomError(ex: Throwable): CustomErrorForSpecs = new CustomErrorForSpecs(ex.getMessage())
  }

}
