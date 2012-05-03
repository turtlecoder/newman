package com.stackmob.newman.response.caching

import java.nio.charset.Charset
import scalaz.Validation
import net.liftweb.json.{NoTypeHints, Serialization}
import com.stackmob.common.validation._
import com.stackmob.newman.response.HttpResponse

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.response.caching
 *
 * User: aaron
 * Date: 5/2/12
 * Time: 11:55 PM
 */

object HttpResponseCaching {
  implicit def bytesToCachedHttpResponse(b: Array[Byte]) = new CachedHttpResponse {
    override val bytes = b
  }

  implicit def httpResponseToCacheable(h: HttpResponse) = new CacheableHttpResponse {
    override val httpResponse = h
  }
}

private[caching] trait ResponseCachingCommon {
  protected val charset = Charset.forName("UTF-8")
}

private[caching] trait CachedHttpResponse extends ResponseCachingCommon {
  import net.liftweb.json.Serialization.read
  def bytes: Array[Byte]

  def toHttpResponse: Validation[Throwable, HttpResponse] = {
    implicit val formats = Serialization.formats(NoTypeHints)
    val string = new String(bytes, charset)
    //TODO: needs moar lift-json-scalaz, especially because HttpResponse will *not* be deserialized correctly
    validating(read[HttpResponse](string))
  }
}

private[caching] trait CacheableHttpResponse extends ResponseCachingCommon {
  import net.liftweb.json.Serialization.write
  def httpResponse: HttpResponse
  def toBytes: Validation[Throwable, Array[Byte]] = validating(write(httpResponse).getBytes(charset))
}
