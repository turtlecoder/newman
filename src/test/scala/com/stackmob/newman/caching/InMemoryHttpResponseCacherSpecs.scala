package com.stackmob.newman.caching

import scalaz._
import Scalaz._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import com.stackmob.newman.{DummyHttpClient, BaseContext}
import com.stackmob.newman.request.HttpRequest.Headers
import com.stackmob.newman.response.HttpResponse
import java.net.URL


/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.caching
 *
 * User: aaron
 * Date: 5/16/12
 * Time: 5:17 PM
 */

class InMemoryHttpResponseCacherSpecs extends Specification { def is =
  "InMemoryHttpResponseCacherSpecs".title                                                                               ^
  "The InMemoryHttpResponseCacher implements an HttpResponseCacher in memory, in a thread-safe manner"                  ^
  "The cacher should"                                                                                                   ^
    "correctly round trip an HttpRequest"                                                                               ! RoundTrip().succeeds ^
                                                                                                                        end
  trait Context extends BaseContext {
    protected val client = new DummyHttpClient()
    protected val request = client.get(new URL("http://stackmob.com"), Headers.empty)
    protected val response = client.responseToReturn
    protected val cache = new InMemoryHttpResponseCacher
  }

  case class RoundTrip() extends Context {
    def succeeds: SpecsResult = {
      (cache.get(request).unsafePerformIO must beEqualTo(Option.empty[HttpResponse])) and
      (cache.exists(request).unsafePerformIO must beFalse) and
      (cache.set(request, response).unsafePerformIO must beEqualTo(())) and
      (cache.get(request).unsafePerformIO must beEqualTo(response.some)) and
      (cache.exists(request).unsafePerformIO must beTrue)
    }
  }
}
