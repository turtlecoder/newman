package com.stackmob.newman.request

import java.net.URL
import _root_.scalaz._
import Scalaz._
import scalaz.effects._
import scalaz.concurrent._
import com.stackmob.newman.response._
import java.nio.charset.Charset
import com.stackmob.newman.Constants._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.common.validation._
import com.stackmob.newman.{Constants, HttpClient}
import com.stackmob.newman.request.HttpRequestExecution._
import java.security.MessageDigest

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.barney.service.filters.proxy
 *
 * User: aaron
 * Date: 4/24/12
 * Time: 4:58 PM
 */


trait HttpRequest {
  import HttpRequest._
  import Headers._
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

  lazy val hash = {
    val headersString = headers.shows
    val bodyBytes = Option(this).collect { case t: HttpRequestWithBody => t.body } | HttpRequestWithBody.RawBody.empty
    val bodyString = new String(bodyBytes, Constants.UTF8Charset)
    val bytes = "%s%s%s".format(url.toString, headersString, bodyString).getBytes(Constants.UTF8Charset)
    md5.digest(bytes)
  }

  def andThen(remainingRequests: NonEmptyList[HttpResponse => HttpRequest]) = chainedRequests(this, remainingRequests)

  def concurrentlyWith(otherRequests: NonEmptyList[HttpRequest]) = concurrentRequests(nel(this, otherRequests.list))
}

object HttpRequest {

  type Header = (String, String)
  type HeaderList = NonEmptyList[Header]
  type Headers = Option[HeaderList]

  object Headers {
    implicit val HeadersEqual = new Equal[Headers] {
      override def equal(headers1: Headers, headers2: Headers) = (headers1, headers2) match {
        case (Some(h1), Some(h2)) => h1.list === h2.list
        case (None, None) => true
        case _ => false
      }
    }

    implicit val HeadersZero = new Zero[Headers] {
      override val zero = Headers.empty
    }

    implicit val HeadersShow = new Show[Headers] {
      override def show(h: Headers) = {
        val s = h.map { headerList: HeaderList =>
          headerList.list.map(h => "%s=%s".format(h._1, h._2)).mkString("&")
        } | ""
        s.toList
      }
    }

    def apply(h: Header): Headers = Headers(nel(h))
    def apply(h: Header, tail: Header*): Headers = Headers(nel(h, tail.toList))
    def apply(h: HeaderList): Headers = h.some
    def apply(h: List[Header]): Headers = h.toNel
    def empty = Option.empty[HeaderList]
  }

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
  import HttpRequestWithBody._
  def body: RawBody
}

object HttpRequestWithBody {
  type RawBody = Array[Byte]
  object RawBody {
    private lazy val emptyBytes = Array[Byte]()

    implicit val RawBodyZero = new Zero[RawBody] {
      override val zero = RawBody.empty
    }

    def empty = emptyBytes
    def apply(s: String, charset: Charset = UTF8Charset) = s.getBytes(charset)
    def apply(b: Array[Byte]) = b
  }

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
