package com.stackmob.newman

import org.apache.http.client
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams
import scalaz.effects._
import org.apache.http.util.EntityUtils
import java.net.URL
import org.apache.http.client.methods._
import scalaz._
import Scalaz._
import org.apache.http.entity.{ByteArrayEntity, BufferedHttpEntity}
import org.apache.http.HttpHeaders._

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
  val connectionTimeout = 5000
  val socketTimeout = 30000

  private[ApacheHttpClient] trait Executor {
    protected def httpMessage: HttpRequestBase
    protected def headers: HeaderList

    private[Executor] def getHttpClient = {
      val client = new DefaultHttpClient
      val httpParams = client.getParams
      HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
      HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
      client
    }

    def execute = {
      val client = getHttpClient

      headers.foreach { list: NonEmptyList[(String, String)] =>
        list.foreach {tup: (String, String) =>
          if(!tup._1.equalsIgnoreCase(CONTENT_LENGTH)) {
            httpMessage.addHeader(tup._1, tup._2)
          }
        }
      }
      httpMessage.setURI(httpMessage.getURI)

      io {
        try {
          val apacheResponse = client.execute(httpMessage)
          val responseCode = HttpResponseStatus.valueOf(apacheResponse.getStatusLine.getStatusCode)
          val headers = apacheResponse.getAllHeaders.map(h => (h.getName, h.getValue)).toList
          val body = Option(apacheResponse.getEntity).map(new BufferedHttpEntity(_)).map(EntityUtils.toByteArray(_))
          HttpResponse(responseCode, headers.toNel, body | Array[Byte]())
        } finally {
          client.getConnectionManager.shutdown()
        }
      }
    }
  }

  case class Get(override val url: URL, override val headers: HeaderList)
    extends GetRequest with Executor {
    override protected val httpMessage = new HttpGet(url.toURI)
  }
  case class Post(override val url: URL, override val headers: HeaderList, override val body: Array[Byte])
    extends PostRequest with Executor {
    override protected val httpMessage = new HttpPost(url.toURI)
    httpMessage.setEntity(new ByteArrayEntity(body))
  }
  case class Put(override val url: URL, override val headers: HeaderList, override val body: Array[Byte])
    extends PutRequest with Executor {
    override protected val httpMessage = new HttpPut(url.toURI)
    httpMessage.setEntity(new ByteArrayEntity(body))
  }
  case class Delete(override val url: URL, override val headers: HeaderList)
    extends DeleteRequest with Executor {
    override protected val httpMessage = new HttpDelete(url.toURI)
  }
  case class Head(override val url: URL, override val headers: HeaderList)
    extends HeadRequest with Executor {
    override protected val httpMessage = new HttpHead(url.toURI)
  }

  override def get(url: URL, headers: HeaderList) = Get(url, headers)
  override def post(url: URL, headers: HeaderList, body: Array[Byte]) = Post(url, headers, body)
  override def put(url: URL, headers: HeaderList, body: Array[Byte]) = Put(url, headers, body)
  override def delete(url: URL, headers: HeaderList) = Delete(url, headers)
  override def head(url: URL, headers: HeaderList) = Head(url, headers)
}
