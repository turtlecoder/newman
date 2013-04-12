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
import Scalaz._
import scalaz.effects._
import scalaz.concurrent._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.newman.{Constants, HttpClient}
import com.stackmob.newman.request.HttpRequestExecution._
import java.security.MessageDigest
import com.stackmob.newman.response._


trait HttpRequest {
  def url: URL
  def requestType: HttpRequestType
  def headers: Headers

  /**
   * prepares an IO that represents executing the HTTP request and returning the response
   * @return an IO representing the HTTP request that executes in the calling thread and
   *         returns the resulting HttpResponse
   */
  def prepare: IO[HttpResponse] = prepareAsync.map(_.get)

  /**
   * prepares an IO that represents a promise that executes the HTTP request and returns the response
   * @return an IO representing the HTTP request that executes in a promise and returns the resulting HttpResponse
   */
  //this needs to be abstract - it is the "root" of the prepare* and execute*Unsafe functions
  def prepareAsync: IO[Promise[HttpResponse]]

  /**
   * alias for prepare.unsafePerformIO. executes the HTTP request immediately in the calling thread
   * @return the HttpResponse that was returned from this HTTP request
   */
  def executeUnsafe: HttpResponse = prepare.unsafePerformIO

  /**
   * alias for prepareAsync.unsafePerformIO. executes the HTTP request in a Promise
   * @return a promise representing the HttpResponse that was returned from this HTTP request
   */
  def executeAsyncUnsafe: Promise[HttpResponse] = prepareAsync.unsafePerformIO

  def toJValue(implicit client: HttpClient): JValue = {
    import net.liftweb.json.scalaz.JsonScalaz.toJSON
    import com.stackmob.newman.serialization.request.HttpRequestSerialization
    val requestSerialization = new HttpRequestSerialization(client)
    toJSON(this)(requestSerialization.writer)
  }

  def toJson(prettyPrint: Boolean = false)(implicit client: HttpClient): String = if(prettyPrint) {
    pretty(render(toJValue))
  } else {
    compact(render(toJValue))
  }

  private lazy val md5 = MessageDigest.getInstance("MD5")

  lazy val hash: List[Byte] = {
    val headersString: String = {
      ~headers.map { hdrs =>
        hdrs.list.foldLeft(new StringBuilder) { (b, h) =>
          b.append(h._1).append(h._2)
        }.toString()
      }
    }
    val bodyBytes = Option(this).collect { case t: HttpRequestWithBody => t.body } | RawBody.empty
    val bodyString = new String(bodyBytes, Constants.UTF8Charset)
    //requestType-url-headers-body
    val bytes = (new StringBuilder)
      .append(requestType.stringVal)
      .append(url.toString)
      .append(headersString)
      .append(bodyString)
      .toString().getBytes(Constants.UTF8Charset)
    md5.digest(bytes).toList
  }

  def andThen(remainingRequests: NonEmptyList[HttpResponse => HttpRequest]): IO[RequestResponsePairList] = {
    chainedRequests(this, remainingRequests)
  }

  def concurrentlyWith(otherRequests: NonEmptyList[HttpRequest]): IO[RequestPromiseResponsePairList] = {
    concurrentRequests(nel(this, otherRequests.list))
  }
}

object HttpRequest {

  def fromJValue(jValue: JValue)(implicit client: HttpClient): Result[HttpRequest] = {
    import com.stackmob.newman.serialization.request.HttpRequestSerialization
    import net.liftweb.json.scalaz.JsonScalaz.fromJSON
    val requestSerialization = new HttpRequestSerialization(client)
    fromJSON(jValue)(requestSerialization.reader)
  }

  def fromJson(json: String)(implicit client: HttpClient): Result[HttpRequest] = (validating {
    parse(json)
  } mapFailure { t: Throwable =>
    UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
  }).liftFailNel.flatMap(fromJValue(_))
}

sealed trait HttpRequestWithBody extends HttpRequest {
  def body: RawBody
}

object HttpRequestWithBody {


}

trait PostRequest extends HttpRequestWithBody {
  override val requestType = HttpRequestType.POST
}

trait PutRequest extends HttpRequestWithBody {
  override val requestType = HttpRequestType.PUT
}

sealed trait HttpRequestWithoutBody extends HttpRequest
trait DeleteRequest extends HttpRequestWithoutBody {
  override val requestType = HttpRequestType.DELETE
}

trait HeadRequest extends HttpRequestWithoutBody {
  override val requestType = HttpRequestType.HEAD
}

trait GetRequest extends HttpRequestWithoutBody {
  override val requestType = HttpRequestType.GET
}
