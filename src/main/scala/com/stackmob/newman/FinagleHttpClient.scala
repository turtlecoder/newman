package com.stackmob.newman

import java.net.URL
import com.stackmob.newman.request._
import scalaz.effects._
import scalaz.concurrent.{Strategy, Promise}
import com.stackmob.newman.response.HttpResponse
import com.twitter.util.{Future => TwitterFuture}
import com.twitter.finagle.http._
import com.twitter.finagle.builder.ClientBuilder
import org.jboss.netty.handler.codec.http.{HttpResponse => NettyHttpResponse,
  HttpRequest => NettyHttpRequest,
  DefaultHttpRequest,
  HttpVersion => NettyHttpVersion,
  HttpMethod => NettyHttpMethod}
import java.nio.ByteBuffer
import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer


/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 4/19/13
 * Time: 10:35 PM
 */
class FinagleHttpClient extends HttpClient {
  import FinagleHttpClient._

  override def get(u: URL, h: Headers): GetRequest = new GetRequest {
    override lazy val url = u
    override lazy val headers = h
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      io(executeRequest(NettyHttpMethod.GET, url, headers))
    }
  }

  override def post(u: URL, h: Headers, b: RawBody): PostRequest = new PostRequest {
    override lazy val url = u
    override lazy val headers = h
    override lazy val body = b
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      io(executeRequest(NettyHttpMethod.POST, url, headers, Some(body)))
    }
  }

  override def put(u: URL, h: Headers, b: RawBody): PutRequest = new PutRequest {
    override lazy val url = u
    override lazy val headers = h
    override lazy val body = b
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      io(executeRequest(NettyHttpMethod.PUT, url, headers, Some(body)))
    }
  }

  override def delete(u: URL, h: Headers): DeleteRequest = new DeleteRequest {
    override lazy val url = u
    override lazy val headers = h
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      io(executeRequest(NettyHttpMethod.DELETE, url, headers))
    }
  }

  override def head(u: URL, h: Headers): HeadRequest = new HeadRequest {
    override lazy val url = u
    override lazy val headers = h
    override def prepareAsync: IO[Promise[HttpResponse]] = {
      io(executeRequest(NettyHttpMethod.HEAD, url, headers))
    }
  }
}

object FinagleHttpClient {

  private[FinagleHttpClient] def executeRequest(method: NettyHttpMethod,
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
      res.toNewmanHttpResponse
    }
  }

  private[FinagleHttpClient] def createClient(url: URL) = {
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

  private[FinagleHttpClient] def createNettyHttpRequest(method: NettyHttpMethod,
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

  private[FinagleHttpClient] def createNettyHttpRequest(method: NettyHttpMethod,
                                                        url: URL,
                                                        headers: Headers,
                                                        body: RawBody): NettyHttpRequest = {
    val req = createNettyHttpRequest(method, url, headers)
    val byteBuffer = ByteBuffer.wrap(body)
    req.setContent(new ByteBufferBackedChannelBuffer(byteBuffer))
    req
  }

  private[FinagleHttpClient] sealed class NettyHttpResponseW(resp: NettyHttpResponse) {
    def toNewmanHttpResponse: HttpResponse = {
      //TODO: implement
      sys.error("not yet implemented")
    }
  }
  private[FinagleHttpClient] implicit def nettyHttpResponseToW(resp: NettyHttpResponse): NettyHttpResponseW = {
    new NettyHttpResponseW(resp)
  }

  private[FinagleHttpClient] sealed class TwitterFutureW[T](future: TwitterFuture[T]) {
    def toScalaPromise: Promise[T] = {
      //use a naive strategy here so that the promise creation doesn't use a thread
      //TODO: I'm not sure if this works
      implicit val strategy = Strategy.Naive
      val promise = new Promise[T]()
      future.onSuccess { res =>
        promise.fulfill(res)
      }.onFailure { t =>
        promise.break
      }
      promise
    }
  }
  private[FinagleHttpClient] implicit def twitterFutureToW[T](future: TwitterFuture[T]): TwitterFutureW[T] = {
    new TwitterFutureW[T](future)
  }
}
