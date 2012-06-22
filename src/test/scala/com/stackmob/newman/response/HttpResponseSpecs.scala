package com.stackmob.newman.response

import scalaz._
import Scalaz._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult, Failure => SpecsFailure}
import com.stackmob.newman.request.HttpRequest.Headers
import com.stackmob.newman.BaseContext
import com.stackmob.newman.request.HttpRequestWithBody.RawBody
import com.stackmob.common.util.casts._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 5/15/12
 * Time: 8:03 PM
 */

class HttpResponseSpecs extends Specification { def is =
  "HttpResponseSpecs".title                                                                                             ^
  """
  HttpResponse is the standard representation in Newman of an Http response
  """                                                                                                                   ^
  "bodyAsIfResponseCode should"                                                                                         ^
    "correctly return an UnexpectedResponseCode if the returned code was not expected"                                  ! UnexpectedResponseCode().correctlyFails ^
    "correctly return the exception returned from the decoder"                                                          ! FailureDecodingBody().correctlyFails ^
    "correectly return the value returned from the decoder"                                                             ! SuccessDecodingBody().succeeds ^
    "correctly return a failure if the decoder threw"                                                                   ! ThrowsDecodingBody().fails ^
                                                                                                                        end

  trait Context extends BaseContext

  case class UnexpectedResponseCode() extends Context {
    protected val expectedRespCode = HttpResponseCode.Ok
    protected val actualRespCode = HttpResponseCode.Unauthorized
    protected val resp = HttpResponse(actualRespCode, Headers.empty, RawBody.empty)
    def correctlyFails: SpecsResult = resp.bodyAsIfResponseCode[String](expectedRespCode, { resp: HttpResponse =>
      (new Exception("shouldn't be thrown")).fail
    }).map { _: String =>
      SpecsFailure("bodyAsIfResponseCode succeeded when it should have failed")
    } ||| { t: Throwable =>
      (t must beAnInstanceOf[HttpResponse.UnexpectedResponseCode]) and
      (t.cast[HttpResponse.UnexpectedResponseCode].map { ex: HttpResponse.UnexpectedResponseCode =>
        {
          (ex.expected must beEqualTo(expectedRespCode)) and
          (ex.actual must beEqualTo(actualRespCode))
        }: SpecsResult
      } | (SpecsFailure("returned excpetion was not an HttpResponse.UnexpectedResponseCode"): SpecsResult))
    }
  }

  case class FailureDecodingBody() extends Context {
    protected val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    protected val decodingEx = new Exception("test decoding exception")
    def correctlyFails: SpecsResult = resp.bodyAsIfResponseCode[String](HttpResponseCode.Ok, { r: HttpResponse =>
      decodingEx.fail
    }).map { s: String =>
      SpecsFailure("bodyAsIfResponseCode succeeded when it should have failed")
    } ||| { t: Throwable =>
      t must beEqualTo(decodingEx)
    }
  }

  case class SuccessDecodingBody() extends Context {
    protected val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    protected val decodedBody = "testResp"
    def succeeds: SpecsResult = resp.bodyAsIfResponseCode(HttpResponseCode.Ok, r => decodedBody.success).map { s: String =>
      (s must beEqualTo(decodedBody)): SpecsResult
    } ||| (logAndFail(_))
  }

  case class ThrowsDecodingBody() extends Context {
    private val ex = new Exception("test exception")
    private val resp = HttpResponse(HttpResponseCode.Ok, Headers.empty, RawBody.empty)
    def fails: SpecsResult = resp.bodyAsIfResponseCode(resp.code, (_ => throw ex)).map { n: Nothing =>
      SpecsFailure("method succeeded when it should not have")
    } ||| { t: Throwable =>
      t must beEqualTo(ex)
    }
  }
}