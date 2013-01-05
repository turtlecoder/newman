/**
 * Copyright 2013 StackMob
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
import scalaz.effects._
import scalaz.concurrent._
import org.apache.http.params.HttpConnectionParams
import response.HttpResponseCode
import org.apache.http.util.EntityUtils
import java.net.URL
import org.apache.http.client.methods._
import org.apache.http.entity.{ByteArrayEntity, BufferedHttpEntity}
import org.apache.http.HttpHeaders._
import com.stackmob.newman.request._
import HttpRequest._
import HttpRequestWithBody._
import com.stackmob.newman.Exceptions.UnknownHttpStatusCodeException
import com.stackmob.newman.response.HttpResponse
import org.apache.http.impl.client.{AbstractHttpClient, DefaultHttpClient}
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.impl.conn.PoolingClientConnectionManager
import java.util.concurrent.{ThreadFactory, Executors}
import ApacheHttpClient._
import java.util.concurrent.atomic.AtomicInteger

class ApacheHttpClient(val socketTimeout: Int = 30000,
                       val connectionTimeout: Int = 5000,
                       val strategy: Strategy = Strategy.Executor(newmanThreadPool)) extends HttpClient {

  val connManager: ClientConnectionManager = {
    val cm = new PoolingClientConnectionManager()
    cm.setDefaultMaxPerRoute(20)
    cm.setMaxTotal(100)
    cm
  }

  private val httpClient: AbstractHttpClient = {
    val client = new DefaultHttpClient(connManager)
    val httpParams = client.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
    HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
    client
  }

  private def wrapIOPromise[T](t: => T): IO[Promise[T]] = io(promise(t)(strategy))

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
    (body <|*|> httpMessage.cast[HttpEntityEnclosingRequestBase]).foreach { tup: (RawBody, HttpEntityEnclosingRequestBase) =>
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

  override def get(u: URL, h: Headers) = new GetRequest {
    override val headers = h
    override val url = u
    override def prepareAsync = executeRequest(new HttpGet, url, headers)
  }

  override def post(u: URL, h: Headers, b: RawBody) = new PostRequest {
    override val url = u
    override val headers = h
    override val body = b
    override def prepareAsync = executeRequest(new HttpPost, url, headers, Option(body))
  }

  override def put(u: URL, h: Headers, b: RawBody) = new PutRequest {
    override val url = u
    override val headers = h
    override val body = b
    override def prepareAsync = executeRequest(new HttpPut, url, headers, Option(body))
  }

  override def delete(u: URL, h: Headers) = new DeleteRequest {
    override val url = u
    override val headers = h
    override def prepareAsync = executeRequest(new HttpDelete, url, headers)
  }

  override def head(u: URL, h: Headers) = new HeadRequest {
    override val url = u
    override val headers = h
    override def prepareAsync = executeRequest(new HttpHead, url, headers)
  }
}

object ApacheHttpClient {
  private val threadNumber = new AtomicInteger(1)
  lazy val newmanThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {

    override def newThread(r: Runnable): Thread = {
      new Thread(r, "newman-" + threadNumber.getAndIncrement)
    }
  })
}
