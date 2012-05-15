package com.stackmob.newman.request

import java.net.URL
import _root_.scalaz._
import Scalaz._
import _root_.scalaz.effects._
import com.stackmob.newman.response._
import java.nio.charset.Charset
import com.stackmob.newman.Constants._
import net.liftweb.json._
import com.stackmob.newman.HttpClient
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.common.validation._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.barney.service.filters.proxy
 *
 * User: aaron
 * Date: 4/24/12
 * Time: 4:58 PM
 */


sealed trait HttpRequest {
  import HttpRequest._
  def url: URL
  def requestType: HttpRequestType
  def headers: Headers
  def execute: IO[HttpResponse]

  def toJValue(implicit client: HttpClient): JValue = {
    import net.liftweb.json.scalaz.JsonScalaz.toJSON
    import com.stackmob.newman.serialization.request.HttpRequestSerialization
    val requestSerialization = new HttpRequestSerialization(client)
    toJSON(this)(requestSerialization.writer)
  }

  def toJson(prettyPrint: Boolean = false)(implicit client: HttpClient) = if(prettyPrint) {
    pretty(render(toJValue))
  } else {
    compact(render(toJValue))
  }
}

object HttpRequest {
  type Header = (String, String)
  type HeaderList = NonEmptyList[Header]
  type Headers = Option[HeaderList]

  object Headers {
    def apply(h: Header): Headers = nel(h).some
    def apply(h: Header, tail: Header*): Headers = nel(h, tail.toList).some
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

  def fromJson(json: String)(implicit client: HttpClient) = validating({
    parse(json)
  }).mapFailure({ t: Throwable =>
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
