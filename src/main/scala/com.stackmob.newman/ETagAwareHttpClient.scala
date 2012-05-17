package com.stackmob.newman

import com.stackmob.newman.request._
import scalaz._
import Scalaz._
import com.stackmob.newman.caching.HttpResponseCacher
import request.HttpRequest._
import response.HttpResponse
import scalaz.effects.IO
import org.apache.http.HttpHeaders
import java.net.URL
import com.stackmob.newman.request.HttpRequestWithBody.RawBody

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 5/15/12
 * Time: 4:21 PM
 */

class ETagAwareHttpClient(httpClient: HttpClient, httpResponseCacher: HttpResponseCacher) extends HttpClient {
  import ETagAwareHttpClient._

  override def get(u: URL, h: Headers) = new GetRequest with CachingMixin {
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.get(u, h).prepare
    override val url = u
    override val headers = h
  }

  override def post(u: URL, h: Headers, b: RawBody) = new PostRequest with CachingMixin {
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.post(u, h, b).prepare
    override val url = u
    override val headers = h
    override val body = b
  }

  override def put(u: URL, h: Headers, b: RawBody) = new PutRequest with CachingMixin {
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.put(u, h, b).prepare
    override val url = u
    override val headers = h
    override val body = b
  }

  override def delete(u: URL, h: Headers) = new DeleteRequest with CachingMixin {
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.delete(u, h).prepare
    override val url = u
    override val headers = h
  }

  override def head(u: URL, h: Headers) = new HeadRequest with CachingMixin {
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.head(u, h).prepare
    override val url = u
    override val headers = h
  }
}

object ETagAwareHttpClient {
  trait CachingMixin { this: HttpRequest =>
    protected def cache: HttpResponseCacher
    protected def doHttpRequest(headers: Headers): IO[HttpResponse]
    private lazy val cacheResult = cache.get(this)

    private def addIfNoneMatch(h: Headers, eTag: String): Headers = h.map { headerList: HeaderList =>
      nel(HttpHeaders.IF_NONE_MATCH -> eTag, headerList.list.filterNot(_._1 === HttpHeaders.IF_NONE_MATCH))
    } orElse { Headers(HttpHeaders.IF_NONE_MATCH -> eTag) }

    def prepare: IO[HttpResponse] = cacheResult.flatMap { cachedResponseOpt: Option[HttpResponse] =>
      cachedResponseOpt some { cachedResponse: HttpResponse =>
        cachedResponse.etag some { eTag: String =>
          val newHeaderList = addIfNoneMatch(this.headers, eTag)
          doHttpRequest(newHeaderList).flatMap { response: HttpResponse =>
          //not modified returned - so return cached response
            if(response.notModified) {
              cachedResponse.pure[IO]
            }
            //not modified was not returned, so cache new response and return it
            else {
              for {
                _ <- cache.set(this, response)
              } yield response
            }
          }
        } none {
          //no etag was present so get the new response, cache it, and return it
          for {
            resp <- doHttpRequest(headers)
            _ <- cache.set(this, resp)
          } yield resp
        }
      } none {
        //no cached response was present, so get the new response, cache it, and return it
        for {
          resp <- doHttpRequest(headers)
          _ <- cache.set(this, resp)
        } yield resp
      }
    }
  }
}
