package com.stackmob.newman

import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import DSL._
import java.net.URL
import response.{HttpResponse, HttpResponseCode}

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 5:47 PM
 */

class ApacheHttpClientSpecs extends Specification { def is =
  "ApacheHttpClientSpecs".title                                                                                         ^
  """
  ApacheHttpClient is the HttpClient implementation that actually hits the internet
  """                                                                                                                   ^
  "The Client Should"                                                                                                   ^
    "Correctly do GET requests"                                                                                         ! Get().succeeds ^
    "Correctly do POST requests"                                                                                        ! Post().succeeds ^
    "Correctly do PUT requests"                                                                                         ! Put().succeeds ^
    "Correctly do DELETE requests"                                                                                      ! Delete().succeeds ^
    "Correctly do HEAD requests"                                                                                        ! Head().succeeds ^
                                                                                                                        end
  trait Context extends BaseContext {
    protected def execute(t: Builder,
                          code: HttpResponseCode = HttpResponseCode.Ok)
                         (fn: HttpResponse => SpecsResult): SpecsResult = {
      val r = t.executeUnsafe
      r.code must beEqualTo(code) and fn(r)
    }
  }

  case class Get() extends Context {
    def succeeds: SpecsResult = execute(GET(new URL("http://stackmob.com"))) { h: HttpResponse =>
      h.bodyString() must contain("html")
    }
  }

  case class Post() extends Context {
    def succeeds: SpecsResult = {
      success
    }
  }

  case class Put() extends Context {
    def succeeds: SpecsResult = {
      success
    }
  }

  case class Delete() extends Context {
    def succeeds: SpecsResult = {
      success
    }
  }

  case class Head() extends Context {
    def succeeds: SpecsResult = {
      success
    }
  }

}
