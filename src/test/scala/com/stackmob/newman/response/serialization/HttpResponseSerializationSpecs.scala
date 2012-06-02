package com.stackmob.newman.response.serialization

import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult, Failure => SpecsFailure}
import com.stackmob.newman.BaseContext
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import com.stackmob.newman.request.HttpRequest._
import com.stackmob.newman.request.HttpRequestWithBody._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.response.serialization
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 6:29 PM
 */

class HttpResponseSerializationSpecs extends Specification { def is =
  "HttpResponseSerializationSpecs".title                                                                                ^
    """
  HttpResponse serialization is responsible for changing an HttpResponse into a JValue and back. It can be
  used to serialize an HttpResponse for storage in a cache, for output to the console, etc...
  """                                                                                                                   ^
  "HttpResponseSerialization should"                                                                                    ^
    "round trip correctly"                                                                                              ! RoundTrip().succeeds ^
                                                                                                                        end
  trait Context extends BaseContext {
    protected val headers = Headers("header1" -> "header1")
    protected val body = RawBody("abcd")
    protected val resp = HttpResponse(HttpResponseCode.Ok, headers, RawBody.empty)
  }

  case class RoundTrip() extends Context {
    def succeeds: SpecsResult = {
      val json = resp.toJson(true)
      HttpResponse.fromJson(json).fold(
        success = { deserialized: HttpResponse =>
          (deserialized.code must beEqualTo(resp.code)) and
          (deserialized.headers must haveTheSameHeadersAs(resp.headers)) and
          (deserialized.rawBody.toList must haveTheSameElementsAs(resp.rawBody.toList))
        },
        failure = { e =>
          SpecsFailure("deserialization failed with error %s".format(e.toString()))
        }
      )
    }
  }

}
