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
import spray.http.{HttpRequest => SprayHttpRequest,
  HttpResponse => SprayHttpResponse,
  HttpMethod => SprayHttpMethod,
  HttpMethods => SprayHttpMethods,
  HttpBody => SprayHttpBody,
  ContentType => SprayContentType,
  HttpEntity => SprayHttpEntity,
  EmptyEntity => SprayEmptyEntity,
  MediaTypes => SprayMediaTypes,
  MediaType => SprayMediaType}
import spray.can.client.{HttpClient => NativeSprayHttpClient}
import spray.client.HttpConduit
import spray.http.HttpHeaders.RawHeader
import spray.io.IOExtension
import java.net.URL
import com.stackmob.newman.request._
import com.stackmob.newman.response._
import scalaz.effect.IO
import scalaz.concurrent.{Strategy, Promise}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import com.stackmob.newman.response.HttpResponse
import scalaz.NonEmptyList
import java.util.UUID

class SprayHttpClient(actorSystem: ActorSystem = SprayHttpClient.DefaultActorSystem,
                      defaultMediaType: SprayMediaType = SprayMediaTypes.`application/json`,
                      defaultContentType: SprayContentType = SprayContentType(SprayMediaTypes.`application/json`)) extends HttpClient {

  import SprayHttpClient._

  private lazy val ioBridge = IOExtension(actorSystem).ioBridge()
  private lazy val httpClient = {
    val clientProps = Props(new NativeSprayHttpClient(ioBridge))
    actorSystem.actorOf(props = clientProps, name = s"http-client-${UUID.randomUUID()}")
  }

  private def pipeline(url: URL): SprayHttpRequest => Future[SprayHttpResponse] = {
    val (host, port) = url.hostAndPort
    val conduit = {
      val conduitProps = Props(new HttpConduit(httpClient, host, port))
      actorSystem.actorOf(props = conduitProps, name = s"http-conduit-$host:$port-${UUID.randomUUID()}")
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
        RawHeader(hdr._1, hdr._2)
      }
    }.getOrElse(Nil)

    val entity: SprayHttpEntity = if(rawBody.length == 0) {
      SprayEmptyEntity
    } else {
      val contentType = headers.getContentType(defaultMediaType)
      SprayHttpBody(contentType, rawBody)
    }

    //we call parseQuery and parseHeaders here so that the caller of this function get the request into the proper state
    val (_, req) = SprayHttpRequest(method, url.getPath, headerList, entity).parseQuery.parseHeaders
    req
  }

  def get(url: URL, headers: Headers) = GetRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.GET, url, headers)
      pipeline(url).executeToNewmanPromise(req, defaultContentType)
    }
  }

  def post(url: URL, headers: Headers, body: RawBody) = PostRequest(url, headers, body) {
    IO {
      val req = request(SprayHttpMethods.POST, url, headers, body)
      pipeline(url).executeToNewmanPromise(req, defaultContentType)
    }
  }

  def put(url: URL, headers: Headers, body: RawBody) = PutRequest(url, headers, body) {
    IO {
      val req = request(SprayHttpMethods.PUT, url, headers, body)
      pipeline(url).executeToNewmanPromise(req, defaultContentType)
    }
  }

  def delete(url: URL, headers: Headers) = DeleteRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.DELETE, url, headers)
      pipeline(url).executeToNewmanPromise(req, defaultContentType)
    }
  }

  def head(url: URL, headers: Headers) = HeadRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.HEAD, url, headers)
      pipeline(url).executeToNewmanPromise(req, defaultContentType)
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

  implicit class RichScalaFuture[T](fut: Future[T]) {
    def toScalazPromise: Promise[T] = {
      val promise = Promise.emptyPromise[T](Strategy.Sequential)
      fut.map { result =>
        promise.fulfill(result)
      }.onFailure {
        case t: Throwable => promise.fulfill(throw t)
      }
      promise
    }
  }

  implicit class RichHeaders(headers: Headers) {
    def getContentType(defaultMediaType: SprayMediaType): SprayContentType = {
      val mbContentTypeHeader = headers.flatMap { lst: HeaderList =>
        lst.list.find { hdr =>
          val (name, _) = hdr
          name.toLowerCase === "content-type"
        }
      }

      val mediaType: SprayMediaType = mbContentTypeHeader.map { header =>
        val (_, value) = header
        value.split("/").toList match {
          case mainType :: subType :: Nil => SprayMediaTypes.CustomMediaType(mainType, subType)
          case mainType :: Nil => SprayMediaTypes.CustomMediaType(mainType, "")
          case _ => defaultMediaType
        }
      } | {
        defaultMediaType
      }

      SprayContentType(mediaType, None)
    }
  }

  private[SprayHttpClient] implicit class RichSprayHttpResponse(resp: SprayHttpResponse) {
    def toNewmanHttpResponse(defaultContentType: SprayContentType): Option[HttpResponse] = {
      for {
        code <- HttpResponseCode.fromInt(resp.status.value)
        rawHeaders <- Option(resp.headers)
        headers <- Option {
          rawHeaders.map { hdr =>
            hdr.name -> hdr.value
          }.toNel
        }
        entity <- Option(resp.entity)
        body <- Option(entity.buffer)
      } yield {
        val contentType = entity.some.collect {
          case SprayHttpBody(cType, _) => "Content-Type" -> cType.value
        } | {
          "Content-Type" -> defaultContentType.value
        }

        val headersPlusContentType = headers.map { hdrNel =>
          hdrNel.<::(contentType)
        } | {
          NonEmptyList(contentType)
        }

        HttpResponse(code, headersPlusContentType.some, body)
      }
    }
  }

  private[SprayHttpClient] implicit class RichPipeline(pipeline: SprayHttpRequest => Future[SprayHttpResponse]) {
    def executeToNewmanPromise(req: SprayHttpRequest,
                               defaultContentType: SprayContentType): Promise[HttpResponse] = {
      pipeline(req).map { res =>
        res.toNewmanHttpResponse(defaultContentType) | (throw new InvalidSprayResponse(res.status.value))
      }.toScalazPromise
    }
  }

  private[SprayHttpClient] class InvalidSprayResponse(code: Int) extends Exception(s"Invalid spray HTTP response with code $code")
}
