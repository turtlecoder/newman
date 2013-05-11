package com.stackmob.newman

import java.net.URL
import com.stackmob.newman.request._
import scalaz.concurrent.{Strategy, Promise}
import com.stackmob.newman.response.{HttpResponseCode, HttpResponse}
import com.twitter.util.{Future => TwitterFuture}
import com.twitter.finagle.http._
import com.twitter.finagle.builder.ClientBuilder
import org.jboss.netty.handler.codec.http.{HttpResponse => NettyHttpResponse, HttpRequest => NettyHttpRequest, HttpVersion => NettyHttpVersion, HttpMethod => NettyHttpMethod, HttpResponseStatus, DefaultHttpRequest}
import java.nio.ByteBuffer
import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer
import scalaz.effect.IO
import scalaz.Scalaz._
import collection.JavaConverters._

class FinagleHttpClient extends HttpClient {
  import FinagleHttpClient._

  override def get(u: URL, h: Headers): GetRequest = new GetRequest {
    override lazy val url = u
    override lazy val headers = h
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      IO(executeRequest(NettyHttpMethod.GET, url, headers))
    }
  }

  override def post(u: URL, h: Headers, b: RawBody): PostRequest = new PostRequest {
    override lazy val url = u
    override lazy val headers = h
    override lazy val body = b
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      IO(executeRequest(NettyHttpMethod.POST, url, headers, Some(body)))
    }
  }

  override def put(u: URL, h: Headers, b: RawBody): PutRequest = new PutRequest {
    override lazy val url = u
    override lazy val headers = h
    override lazy val body = b
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      IO(executeRequest(NettyHttpMethod.PUT, url, headers, Some(body)))
    }
  }

  override def delete(u: URL, h: Headers): DeleteRequest = new DeleteRequest {
    override lazy val url = u
    override lazy val headers = h
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      IO(executeRequest(NettyHttpMethod.DELETE, url, headers))
    }
  }

  override def head(u: URL, h: Headers): HeadRequest = new HeadRequest {
    override lazy val url = u
    override lazy val headers = h
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      IO(executeRequest(NettyHttpMethod.HEAD, url, headers))
    }
  }
}

object FinagleHttpClient {

  def executeRequest(method: NettyHttpMethod,
                     url: URL,
                     headers: Headers,
                     mbBody: Option[RawBody] = None): Promise[HttpResponse] = {
    val client = createClient(url)
    val req = mbBody.map { body =>
      createNettyHttpRequest(method, url, headers, body)
    }.getOrElse {
      createNettyHttpRequest(method, url, headers)
    }
    client(req).toScalaPromise.map { res =>
      res.toNewmanHttpResponse | {
        throw new InvalidNettyResponse(res.getStatus)
      }
    }
  }

  def createClient(url: URL) = {
    val host = url.getHost
    val port = url.getPort match {
      case -1 => 80
      case other => other
    }

    ClientBuilder()
      .codec(Http())
      .hosts("%s:%s".format(host, port))
      .hostConnectionLimit(1)
      .build()
  }

  def createNettyHttpRequest(method: NettyHttpMethod,
                             url: URL,
                             headers: Headers): NettyHttpRequest = {
    val request = new DefaultHttpRequest(NettyHttpVersion.HTTP_1_1, method, url.getPath)
    headers.foreach { headerList =>
      headerList.list.foreach { header =>
        request.setHeader(header._1, header._2)
      }
    }
    request
  }

  def createNettyHttpRequest(method: NettyHttpMethod,
                             url: URL,
                             headers: Headers,
                             body: RawBody): NettyHttpRequest = {
    val req = createNettyHttpRequest(method, url, headers)
    val byteBuffer = ByteBuffer.wrap(body)
    req.setContent(new ByteBufferBackedChannelBuffer(byteBuffer))
    req
  }

  implicit class NettyHttpResponseW(resp: NettyHttpResponse) {
    def toNewmanHttpResponse: Option[HttpResponse] = {
      for {
        code <- HttpResponseCode.fromInt(resp.getStatus.getCode)
        rawHeaders <- Option(resp.getHeaders)
        headers <- {
          val tupList = rawHeaders.asScala.map { entry =>
            entry.getKey -> entry.getValue
          }
          Option(tupList.toList.toNel)
        }
        body <- Option(resp.getContent.array)
      } yield {
        HttpResponse(code, headers, body)
      }
    }
  }

  implicit class TwitterFutureW[T](future: TwitterFuture[T]) {
    def toScalaPromise: Promise[T] = {
      val promise = Promise.emptyPromise[T](Strategy.Sequential)
      future.onSuccess { result =>
        promise.fulfill(result)
      }.onFailure { throwable =>
        promise.fulfill(throw throwable)
      }
      promise
    }
  }

  class InvalidNettyResponse(nettyCode: HttpResponseStatus) extends Exception(s"Invalid netty response with code: ${nettyCode.getCode}")
}
