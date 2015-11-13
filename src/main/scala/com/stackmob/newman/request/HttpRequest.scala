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
package request

import java.net.URL
import scalaz._
import scalaz.Validation._
import Scalaz._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.scalaz.JsonScalaz._
import com.stackmob.newman.{Constants, HttpClient}
import com.stackmob.newman.request.HttpRequestExecution._
import java.security.MessageDigest
import com.stackmob.newman.response._
import com.stackmob.newman.caching._
import org.apache.commons.codec.binary.Hex
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration._

trait HttpRequest {
  def url: URL
  def requestType: HttpRequestType
  def headers: Headers

  /**
   * submit this request and return a {{{scala.concurrent.Future}}} that will contain the response
   * @return the Future that represents the running request
   */
  def apply: Future[HttpResponse]

  def toJValue(implicit client: HttpClient): JValue = {
    import org.json4s.scalaz.JsonScalaz.toJSON
    import com.stackmob.newman.serialization.request.HttpRequestSerialization
    val requestSerialization = new HttpRequestSerialization(client)
    toJSON(this)(requestSerialization.writer)
  }

  def toJson(prettyPrint: Boolean = false)(implicit client: HttpClient): String = if(prettyPrint) {
    pretty(render(toJValue))
  } else {
    compact(render(toJValue))
  }

  lazy val md5 = HttpRequest.md5.clone().asInstanceOf[MessageDigest]

  lazy val hash: HashCode = {

    val headersString: String = {
      ~headers.map { hdrs =>
        hdrs.list.foldLeft(new StringBuilder) { (b, h) =>
          b.append(h._1).append(h._2)
        }.toString()
      }
    }

    val bodyBytes = this match {
      case t: HttpRequestWithBody => t.body
      case _ => RawBody.empty
    }

    def combineMessageParts(byteArrays: Array[Byte]*): Array[Byte] = {
      val msgBytes: Array[Byte] = new Array(byteArrays.foldLeft(0)((s, arr) => s + arr.length))

      var currPos = 0
      for (arr <- byteArrays) {
        System.arraycopy(arr, 0, msgBytes, currPos, arr.length)
        currPos += arr.length
      }

      msgBytes
    }

    val requestTypeBytes = requestType.stringVal.getBytes(Constants.UTF8Charset)
    val urlStringBytes = url.toString.getBytes(Constants.UTF8Charset)
    val headersStringBytes = headersString.getBytes(Constants.UTF8Charset)

    Hex.encodeHexString(md5.digest(combineMessageParts(requestTypeBytes, urlStringBytes, headersStringBytes, bodyBytes)))
  }

  /**
   * execute this request first, and then a series of other requests each after the previous finished
   * @param otherRequests the other requests to execute, in the given order
   * @return a list of request / response pairs. each request will start immediately after the previous response has finished.
   *         the first request will start immediately
   */
  def andThen(otherRequests: List[HttpRequest])
             (implicit ctx: ExecutionContext): List[ReqRespFut] = {
    sequencedRequests(this :: otherRequests)
  }

  /**
   * execute this request concurrently with a series of others
   * @param otherRequests the other requests to execute
   * @return a set of all the requests executed (including this one) and the futures representing their responses
   */
  def concurrentlyWith(otherRequests: List[HttpRequest])
                      (implicit ctx: ExecutionContext): Set[ReqRespFut] = {
    concurrentRequests(this :: otherRequests)
  }
}

object HttpRequest {

  private lazy val md5 = MessageDigest.getInstance("MD5")

  def fromJValue(jValue: JValue)(implicit client: HttpClient): Result[HttpRequest] = {
    import com.stackmob.newman.serialization.request.HttpRequestSerialization
    import org.json4s.scalaz.JsonScalaz.fromJSON
    val requestSerialization = new HttpRequestSerialization(client)
    fromJSON(jValue)(requestSerialization.reader)
  }

  def fromJson(json: String)(implicit client: HttpClient): Result[HttpRequest] = (fromTryCatch {
    parse(json)
  } leftMap { t: Throwable =>
    UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
  }).toValidationNel.flatMap { j: JValue => fromJValue(j) }
}

sealed trait HttpRequestWithBody extends HttpRequest {
  def body: RawBody
}

object HttpRequestWithBody {


}

trait PostRequest extends HttpRequestWithBody {
  override val requestType = HttpRequestType.POST
}
object PostRequest {
  def apply(u: URL, h: Headers, r: RawBody)
           (async: => Future[HttpResponse]): PostRequest = new PostRequest {
    override lazy val url = u
    override lazy val headers = h
    override lazy val body = r
    override lazy val apply = async
  }
}

trait PutRequest extends HttpRequestWithBody {
  override val requestType = HttpRequestType.PUT
}
object PutRequest {
  def apply(u: URL, h: Headers, r: RawBody)
           (async: => Future[HttpResponse]): PutRequest = new PutRequest {
    override lazy val url = u
    override lazy val headers = h
    override lazy val body = r
    override lazy val apply = async
  }
}

sealed trait HttpRequestWithoutBody extends HttpRequest
trait DeleteRequest extends HttpRequestWithoutBody {
  override val requestType = HttpRequestType.DELETE
}
object DeleteRequest {
  def apply(u: URL, h: Headers)
           (async: => Future[HttpResponse]): DeleteRequest = new DeleteRequest {
    override lazy val url: URL = u
    override lazy val headers = h
    override lazy val apply = async
  }
}

trait HeadRequest extends HttpRequestWithoutBody {
  override val requestType = HttpRequestType.HEAD
}
object HeadRequest {
  def apply(u: URL, h: Headers)
           (async: => Future[HttpResponse]): HeadRequest = new HeadRequest {
    override lazy val url: URL = u
    override lazy val headers = h
    override lazy val apply = async
  }
}

trait GetRequest extends HttpRequestWithoutBody {
  override val requestType = HttpRequestType.GET
}
object GetRequest {
  def apply(u: URL, h: Headers)
           (async: => Future[HttpResponse]): GetRequest = new GetRequest {
    override lazy val url: URL = u
    override lazy val headers = h
    override lazy val apply = async
  }
}
