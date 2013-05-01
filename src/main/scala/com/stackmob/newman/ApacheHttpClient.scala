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

import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.concurrent._
import org.apache.http.params.HttpConnectionParams
import response.HttpResponseCode
import org.apache.http.util.EntityUtils
import java.net.URL
import org.apache.http.client.methods._
import org.apache.http.entity.{ByteArrayEntity, BufferedHttpEntity}
import org.apache.http.HttpHeaders._
import com.stackmob.newman.request._
import com.stackmob.newman.Exceptions.UnknownHttpStatusCodeException
import com.stackmob.newman.response.HttpResponse
import org.apache.http.impl.client.{AbstractHttpClient, DefaultHttpClient}
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.impl.conn.PoolingClientConnectionManager
import java.util.concurrent.{ThreadFactory, Executors}
import ApacheHttpClient._
import java.util.concurrent.atomic.AtomicInteger

class ApacheHttpClient(val socketTimeout: Int = ApacheHttpClient.DefaultSocketTimeout,
                       val connectionTimeout: Int = ApacheHttpClient.DefaultConnectionTimeout,
                       val maxConnectionsPerRoute: Int = ApacheHttpClient.DefaultMaxConnectionsPerRoute,
                       val maxTotalConnections: Int = ApacheHttpClient.DefaultMaxTotalConnections,
                       val strategy: Strategy = Strategy.Executor(newmanThreadPool)) extends HttpClient {

  private val connManager: ClientConnectionManager = {
    val cm = new PoolingClientConnectionManager()
    cm.setDefaultMaxPerRoute(maxConnectionsPerRoute)
    cm.setMaxTotal(maxTotalConnections)
    cm
  }

  private val httpClient: AbstractHttpClient = {
    val client = new DefaultHttpClient(connManager)
    val httpParams = client.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
    HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
    client
  }

  private def wrapIOPromise[T](t: => T): IO[Promise[T]] = IO(Promise(t)(strategy))

  protected def executeRequest(httpMessage: HttpRequestBase,
                               url: URL,
                               headers: Headers,
                               body: Option[RawBody] = none): IO[Promise[HttpResponse]] = wrapIOPromise {
    httpMessage.setURI(url.toURI)
    headers.foreach { list: NonEmptyList[(String, String)] =>
      list.foreach {tup: (String, String) =>
        if(!tup._1.equalsIgnoreCase(CONTENT_LENGTH)) {
          httpMessage.addHeader(tup._1, tup._2)
        }
      }
    }
    //if there's both a body and httpMessage is an entity enclosing request, then set the body
    (body tuple httpMessage.cast[HttpEntityEnclosingRequestBase]).foreach { tup: (RawBody, HttpEntityEnclosingRequestBase) =>
      val (body,req) = tup
      req.setEntity(new ByteArrayEntity(body))
    }

    val apacheResponse = httpClient.execute(httpMessage)
    val responseCode = HttpResponseCode.fromInt(apacheResponse.getStatusLine.getStatusCode) | {
      throw new UnknownHttpStatusCodeException(apacheResponse.getStatusLine.getStatusCode)
    }
    val responseHeaders = apacheResponse.getAllHeaders.map(h => (h.getName, h.getValue)).toList
    val responseBody = Option(apacheResponse.getEntity).map(new BufferedHttpEntity(_)).map(EntityUtils.toByteArray(_))
    HttpResponse(responseCode, responseHeaders.toNel, responseBody | RawBody.empty)
  }

  override def get(u: URL, h: Headers): GetRequest = new GetRequest {
    override val headers = h
    override val url = u
    override def prepareAsync: IO[Promise[HttpResponse]] = executeRequest(new HttpGet, url, headers)
  }

  override def post(u: URL, h: Headers, b: RawBody): PostRequest = new PostRequest {
    override val url = u
    override val headers = h
    override val body = b
    override def prepareAsync: IO[Promise[HttpResponse]] = executeRequest(new HttpPost, url, headers, Option(body))
  }

  override def put(u: URL, h: Headers, b: RawBody): PutRequest = new PutRequest {
    override val url = u
    override val headers = h
    override val body = b
    override def prepareAsync: IO[Promise[HttpResponse]] = executeRequest(new HttpPut, url, headers, Option(body))
  }

  override def delete(u: URL, h: Headers): DeleteRequest = new DeleteRequest {
    override val url = u
    override val headers = h
    override def prepareAsync: IO[Promise[HttpResponse]] = executeRequest(new HttpDelete, url, headers)
  }

  override def head(u: URL, h: Headers): HeadRequest = new HeadRequest {
    override val url = u
    override val headers = h
    override def prepareAsync: IO[Promise[HttpResponse]] = executeRequest(new HttpHead, url, headers)
  }
}

object ApacheHttpClient {
  private[ApacheHttpClient] val DefaultSocketTimeout = 30000
  private[ApacheHttpClient] val DefaultConnectionTimeout = 5000
  private[ApacheHttpClient] val DefaultMaxConnectionsPerRoute = 20
  private[ApacheHttpClient] val DefaultMaxTotalConnections = 100
  private val threadNumber = new AtomicInteger(1)
  lazy val newmanThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {

    override def newThread(r: Runnable): Thread = {
      new Thread(r, "newman-" + threadNumber.getAndIncrement)
    }
  })
}
