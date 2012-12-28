package com.stackmob.newman
package request.serialization

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.request.serialization
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 6:02 PM
 */

import scalaz._
import Scalaz._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult, Failure => SpecsFailure, Success => SpecsSuccess}
import com.stackmob.newman.request.HttpRequest.Headers
import com.stackmob.newman.request.HttpRequestWithBody.RawBody
import com.stackmob.newman.{DummyHttpClient, BaseContext}
import java.net.URL
import com.stackmob.newman.request.{HttpRequestWithoutBody, HttpRequest, HttpRequestWithBody}

class HttpRequestSerializationSpecs extends Specification { def is =
  "HttpRequestSerializationSpecs".title                                                                                 ^
  """
  HttpRequest serialization is responsible for changing an HttpRequest into a JValue and back. It can be
  used to serialize an HttpRequest for storage in a cache, for output to the console, etc...
  """                                                                                                                   ^
  "HttpRequestSerialization should"                                                                                     ^
    "round trip correctly"                                                                                              ! RoundTrip().succeeds ^
  end

  trait Context extends BaseContext {

    protected lazy val url = new URL("http://stackmob.com")
    protected lazy val headers = Headers("header1" -> "header1")
    protected lazy val body = RawBody("abcd")
    protected lazy val getReq = client.get(url, headers)
    protected lazy val postReq = client.post(url, headers, body)
    protected lazy val putReq = client.put(url, headers, body)
    protected lazy val deleteReq = client.delete(url, headers)
    protected lazy val headReq = client.head(url, headers)
    protected lazy val client = new DummyHttpClient

    private def ensure(t: HttpRequest)(extra: HttpRequest => SpecsResult): SpecsResult = {
      val json = t.toJson(false)(client)
      HttpRequest.fromJson(json)(client).fold(
        success = { deser: HttpRequest =>
          (deser.url must beEqualTo(t.url)) and
          (deser.headers must haveTheSameHeadersAs(t.headers)) and
          (extra(deser))
        },
        failure = { eNel =>
          SpecsFailure("deserialization failed with %d errors".format(eNel.list.length)): SpecsResult
        }
      )
    }

    protected def ensure(t: HttpRequestWithoutBody): SpecsResult = {
      ensure(t: HttpRequest) { r: HttpRequest => SpecsSuccess("ok") }
    }

    protected def ensure(t: HttpRequestWithBody): SpecsResult = {
      ensure(t: HttpRequest) { r: HttpRequest =>
        r.cast[HttpRequestWithBody].map { d: HttpRequestWithBody =>
          (d.body must beEqualTo(t.body)): SpecsResult
        } | SpecsFailure("deserialization returned a %s, not HttpRequestWithBody as expected".format(r.getClass.getCanonicalName))
      }
    }
  }

  case class RoundTrip() extends Context {
    def succeeds: SpecsResult = {
      val getRes = ensure(getReq)
      val postRes = ensure(postReq)
      val putRes = ensure(putReq)
      val deleteRes = ensure(deleteReq)
      val headRes = ensure(headReq)

      getRes and postRes and putRes and deleteRes and headRes
    }
  }

}
