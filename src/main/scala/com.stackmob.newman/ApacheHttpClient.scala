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
import com.stackmob.common.util.casts._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.barney.service.filters.proxy
 *
 * User: aaron
 * Date: 4/24/12
 * Time: 6:28 PM
 */

class ApacheHttpClient extends HttpClient {
  private val connectionTimeout = 5000
  private val socketTimeout = 30000

  private def getHttpClient: AbstractHttpClient = {
    val client = new DefaultHttpClient
    val httpParams = client.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
    HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
    client
  }

  protected def executeRequest(httpMessage: HttpRequestBase,
                               url: URL,
                               headers: Headers,
                               body: Option[RawBody] = none): IO[Promise[HttpResponse]] = io {
    promise {
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

      val client = getHttpClient
      try {
        val apacheResponse = client.execute(httpMessage)
        val responseCode = HttpResponseCode.fromInt(apacheResponse.getStatusLine.getStatusCode) | {
          throw new UnknownHttpStatusCodeException(apacheResponse.getStatusLine.getStatusCode)
        }
        val headers = apacheResponse.getAllHeaders.map(h => (h.getName, h.getValue)).toList
        val body = Option(apacheResponse.getEntity).map(new BufferedHttpEntity(_)).map(EntityUtils.toByteArray(_))
        HttpResponse(responseCode, headers.toNel, body | RawBody.empty)
      } finally {
        client.getConnectionManager.shutdown()
      }
    }
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
    override def prepareAsync = executeRequest(new HttpPost, url, headers, body.some)
  }

  override def put(u: URL, h: Headers, b: RawBody) = new PutRequest {
    override val url = u
    override val headers = h
    override val body = b
    override def prepareAsync = executeRequest(new HttpPut, url, headers, body.some)
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
