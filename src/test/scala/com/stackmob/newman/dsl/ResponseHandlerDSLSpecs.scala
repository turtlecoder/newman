package com.stackmob.newman.dsl

import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import scalaz._
import effects._
import Scalaz._
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.dsl
 *
 * User: aaron
 * Date: 7/6/12
 * Time: 5:52 PM
 */

class ResponseHandlerDSLSpecs extends Specification { def is =
  "ResponseHandlerDSLSpecs".title                                                                                       ^
  """
  ResponseHandlerDSL is a DSL used to handle responses with Newman
  """                                                                                                                   ^
  "The DSL should"                                                                                                      ^
    "return a Failure if the IO throws"                                                                                 ! ThrowingIO().returnsFailure ^
                                                                                                                        end
  trait Context

  import com.stackmob.newman.dsl._

  case class ThrowingIO() extends Context {
    def returnsFailure: SpecsResult = {
      val ex = new Exception("test exception")
      val respIO = io((throw ex): HttpResponse).handleCode(HttpResponseCode.Ok, _ => ().success)
      respIO.unsafePerformIO.either must beLeft.like {
        case e => e must beEqualTo(ex)
      }
    }
  }

}
