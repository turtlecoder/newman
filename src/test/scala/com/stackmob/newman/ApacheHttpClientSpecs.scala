package com.stackmob.newman

import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import dsl._
import java.net.URL
import com.stackmob.newman.response.{HttpResponse, HttpResponseCode}

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
    "Correctly do async GET requests"                                                                                   ! Get().succeedsAsync ^
    "Correctly do POST requests"                                                                                        ! Post().succeeds ^
    "Correctly do async POST requests"                                                                                  ! Post().succeedsAsync ^
    "Correctly do PUT requests"                                                                                         ! Put().succeeds ^
    "Correctly do async PUT requests"                                                                                   ! Put().succeedsAsync ^
    "Correctly do DELETE requests"                                                                                      ! Delete().succeeds ^
    "Correctly do async DELETE requests"                                                                                ! Delete().succeedsAsync ^
    "Correctly do HEAD requests"                                                                                        ! Head().succeeds ^
    "Correctly do async HEAD requests"                                                                                  ! Head().succeedsAsync ^
                                                                                                                        end
  trait Context extends BaseContext {
    implicit protected val httpClient = new ApacheHttpClient

    protected def execute(t: Builder,
                          expectedCode: HttpResponseCode = HttpResponseCode.Ok)
                         (fn: HttpResponse => SpecsResult): SpecsResult = {
      val r = t.executeUnsafe
      r.code must beEqualTo(expectedCode) and fn(r)
    }

    protected def executeAsync(t: Builder,
                               expectedCode: HttpResponseCode = HttpResponseCode.Ok)
                              (fn: HttpResponse => SpecsResult): SpecsResult = {
      val rPromise = t.executeAsyncUnsafe
      rPromise.map { r: HttpResponse =>
        r.code must beEqualTo(expectedCode) and fn(r)
      }.get
    }

    protected lazy val url = new URL("http://stackmob.com")

    implicit private val encoding = Constants.UTF8Charset
    protected def ensureHttpOK(h: HttpResponse): SpecsResult = h.code must beEqualTo(HttpResponseCode.Ok)
    protected def ensureHtmlReturned(h: HttpResponse): SpecsResult = {
      (h.bodyString() must contain("html")) and
      (h.bodyString() must contain("/html"))
    }

    protected def ensureHtmlResponse(h: HttpResponse) = ensureHttpOK(h) and ensureHtmlReturned(h)

  }

  case class Get() extends Context {
    def succeeds = execute(GET(url))(ensureHtmlResponse(_))
    def succeedsAsync = executeAsync(GET(url))(ensureHtmlResponse(_))
  }

  case class Post() extends Context {
    private val post = POST(url)
    def succeeds = execute(post)(ensureHtmlResponse(_))
    def succeedsAsync = executeAsync(post)(ensureHtmlResponse(_))
  }

  case class Put() extends Context {
    private val put = PUT(url)
    def succeeds = execute(put)(ensureHtmlResponse(_))
    def succeedsAsync = executeAsync(put)(ensureHtmlResponse(_))
  }

  case class Delete() extends Context {
    private val delete = DELETE(url)
    def succeeds = execute(delete)(ensureHtmlResponse(_))
    def succeedsAsync = executeAsync(delete)(ensureHtmlResponse(_))
  }

  case class Head() extends Context {
    private val head = HEAD(url)
    def succeeds = execute(head)(ensureHttpOK(_))
    def succeedsAsync = executeAsync(head)(ensureHttpOK(_))
  }

}
