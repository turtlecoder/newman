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

import akka.actor._
import spray.client.HttpConduit
import spray.io._
import spray.http.{HttpRequest => SprayHttpRequest,
  HttpResponse => SprayHttpResponse,
  HttpHeader => SprayHttpHeader,
  HttpMethod => SprayHttpMethod,
  HttpMethods => SprayHttpMethods,
  HttpEntity => SprayHttpEntity,
  EmptyEntity => SprayEmptyEntity}
import spray.can.client.{HttpClient => NativeSprayHttpClient}
import java.net.URL
import com.stackmob.newman.request._
import com.stackmob.newman.response._
import scalaz.effect.IO
import scalaz.concurrent.{Strategy, Promise}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._

class SprayHttpClient(actorSystem: ActorSystem = ActorSystem()) extends HttpClient {
  import SprayHttpClient._

  private lazy val ioBridge = IOExtension(actorSystem).ioBridge()
  private lazy val httpClient = {
    val clientProps = Props(new NativeSprayHttpClient(ioBridge))
    actorSystem.actorOf(props = clientProps, name = s"http-client")
  }

  private def pipeline(url: URL): SprayHttpRequest => Future[SprayHttpResponse] = {
    val (host, port) = url.hostAndPort
    val conduit = {
      val conduitProps = Props(new HttpConduit(httpClient, host, port))
      actorSystem.actorOf(props = conduitProps, name = s"http-conduit-$host:$port")
    }
    HttpConduit.sendReceive(conduit)
  }

  private def request(method: SprayHttpMethod,
                      url: URL,
                      headers: Headers,
                      rawBody: RawBody = RawBody.empty): SprayHttpRequest = {
    val headerList = headers.map { headerNel =>
      val lst = headerNel.list
      lst.map { hdr =>
        new SprayHttpHeader {
          override lazy val name: String = hdr._1
          override lazy val value: String = hdr._2
          override lazy val lowercaseName: String = hdr._2.toLowerCase
        }
      }
    }.getOrElse(Nil)
    val entity = if(rawBody.length == 0) {
      SprayEmptyEntity
    } else {
      SprayHttpEntity(rawBody)
    }
    SprayHttpRequest(method, url.getPath, headerList, entity)
  }

  def get(url: URL, headers: Headers) = GetRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.GET, url, headers)
      pipeline(url).executeToNewmanPromise(req)
    }
  }

  def post(url: URL, headers: Headers, body: RawBody) = PostRequest(url, headers, body) {
    IO {
      val req = request(SprayHttpMethods.POST, url, headers, body)
      pipeline(url).executeToNewmanPromise(req)
    }
  }

  def put(url: URL, headers: Headers, body: RawBody) = PutRequest(url, headers, body) {
    IO {
      val req = request(SprayHttpMethods.PUT, url, headers, body)
      pipeline(url).executeToNewmanPromise(req)
    }
  }

  def delete(url: URL, headers: Headers) = DeleteRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.DELETE, url, headers)
      pipeline(url).executeToNewmanPromise(req)
    }
  }

  def head(url: URL, headers: Headers) = HeadRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.HEAD, url, headers)
      pipeline(url).executeToNewmanPromise(req)
    }
  }
}

object SprayHttpClient {
  private[SprayHttpClient] lazy val DefaultActorSystem = ActorSystem()

  private[SprayHttpClient] implicit val sequentialExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable) {
      runnable.run()
    }
    def reportFailure(t: Throwable) {}
    override lazy val prepare: ExecutionContext = this
  }

  private[SprayHttpClient] implicit class RichFuture[T](fut: Future[T]) {
    def toPromise: Promise[T] = {
      val promise = Promise.emptyPromise[T](Strategy.Sequential)
      fut.map { result =>
        promise.fulfill(result)
      }.onFailure {
        case t: Throwable => promise.fulfill(throw t)
      }
      promise
    }
  }

  private[SprayHttpClient] implicit class RichSprayHttpResponse(resp: SprayHttpResponse) {
    def toNewmanHttpResponse: Option[HttpResponse] = {
      for {
        code <- HttpResponseCode.fromInt(resp.status.value)
        rawHeaders <- Option(resp.headers)
        headers <- Option {
          rawHeaders.map { hdr =>
            hdr.name -> hdr.value
          }.toNel
        }
        body <- Option(resp.entity.buffer)
      } yield {
        HttpResponse(code, headers, body)
      }
    }
  }

  private[SprayHttpClient] implicit class RichPipeline(pipeline: SprayHttpRequest => Future[SprayHttpResponse]) {
    def executeToNewmanPromise(req: SprayHttpRequest): Promise[HttpResponse] = {
      pipeline(req).map {
        res => res.toNewmanHttpResponse | (throw new InvalidSprayResponse(res.status.value))
      }.toPromise
    }
  }

  private[SprayHttpClient] class InvalidSprayResponse(code: Int) extends Exception(s"Invalid spray HTTP response with code $code")
}
