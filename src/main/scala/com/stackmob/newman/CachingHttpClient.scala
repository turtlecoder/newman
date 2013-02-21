package com.stackmob.newman

import scalaz.Scalaz._
import scalaz.effects.IO
import scalaz.concurrent.Promise
import caching.{Time, HttpResponseCacher}
import request._
import response.HttpResponse
import java.net.URL

/**
 * Created by IntelliJ IDEA.
 * 
 * com.stackmob.newman
 * 
 * User: aaron
 * Date: 2/14/13
 * Time: 5:16 PM
 */
class CachingHttpClient(httpClient: HttpClient,
                        httpResponseCacher: HttpResponseCacher,
                        t: Time) extends HttpClient {
  import CachingHttpClient._

  override def get(u: URL, h: Headers): GetRequest = new GetRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.get(u, h).prepare
    override val url = u
    override val headers = h
  }

  override def post(u: URL, h: Headers, b: RawBody): PostRequest = new PostRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.post(u, h, b).prepare
    override val url = u
    override val headers = h
    override val body = b
  }

  override def put(u: URL, h: Headers, b: RawBody): PutRequest = new PutRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.put(u, h, b).prepare
    override val url = u
    override val headers = h
    override val body = b
  }

  override def delete(u: URL, h: Headers): DeleteRequest = new DeleteRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.delete(u, h).prepare
    override val url = u
    override val headers = h
  }

  override def head(u: URL, h: Headers): HeadRequest = new HeadRequest with CachingMixin {
    override protected lazy val ttl = t
    override protected val cache = httpResponseCacher
    override protected def doHttpRequest(h: Headers) = httpClient.head(u, h).prepare
    override val url = u
    override val headers = h
  }
}

object CachingHttpClient {
  trait CachingMixin extends HttpRequest { this: HttpRequest =>
    protected def ttl: Time
    protected def cache: HttpResponseCacher
    protected def doHttpRequest(headers: Headers): IO[HttpResponse]

    override def prepareAsync: IO[Promise[HttpResponse]] = cache.get(this).flatMap { mbCachedResponse: Option[HttpResponse] =>
      mbCachedResponse.map { resp =>
        resp.pure[Promise].pure[IO]
      } | {
        doHttpRequest(headers).flatMap { response: HttpResponse =>
          cache.set(this, response, ttl) >| response.pure[Promise]
        }
      }
    }
  }
}
