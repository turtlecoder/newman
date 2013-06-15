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
import spray.http.{Uri,
  HttpRequest => SprayHttpRequest,
  HttpResponse => SprayHttpResponse,
  HttpMethod => SprayHttpMethod,
  HttpMethods => SprayHttpMethods,
  HttpBody => SprayHttpBody,
  ContentTypes => SprayContentTypes,
  ContentType => SprayContentType,
  HttpEntity => SprayHttpEntity,
  EmptyEntity => SprayEmptyEntity,
  MediaTypes => SprayMediaTypes,
  MediaType => SprayMediaType}
import spray.http.HttpHeaders.RawHeader
import java.net.URL
import com.stackmob.newman.request._
import com.stackmob.newman.response._
import scalaz.effect.IO
import scalaz.concurrent.Promise
import scala.concurrent.Future
import scalaz.Scalaz._
import com.stackmob.newman.response.HttpResponse
import scalaz.NonEmptyList
import akka.io.{IO => AkkaIO}
import akka.pattern.ask
import spray.can.Http
import akka.util.Timeout
import java.util.concurrent.TimeUnit

class SprayHttpClient(actorSystem: ActorSystem = SprayHttpClient.DefaultActorSystem,
                      defaultMediaType: SprayMediaType = SprayMediaTypes.`application/json`,
                      defaultContentType: SprayContentType = SprayContentTypes.`application/json`,
                      timeout: Timeout = Timeout(5, TimeUnit.SECONDS)) extends HttpClient {

  import SprayHttpClient._


  private implicit val clientActorSystem: ActorSystem = actorSystem
  private implicit val clientTimeout: Timeout = timeout

  private def request(method: SprayHttpMethod,
                      url: URL,
                      headers: Headers,
                      rawBody: RawBody = RawBody.empty): SprayHttpRequest = {
    val headerList = headers.map { headerNel =>
      val lst = headerNel.list
      lst.map { hdr =>
        RawHeader(hdr._1, hdr._2)
      }
    } | Nil

    val entity: SprayHttpEntity = {
      if(rawBody.length == 0) {
        SprayEmptyEntity
      } else {
        val contentType = headers.getContentType(defaultMediaType)
        SprayHttpBody(contentType, rawBody)
      }
    }

    SprayHttpRequest(method, Uri(url.toString), headerList, entity)
  }

  override def get(url: URL, headers: Headers) = GetRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.GET, url, headers)
      val resp = (AkkaIO(Http) ? req).mapTo[SprayHttpResponse]
      resp.executeToNewmanPromise(req, defaultContentType)
    }
  }

  override def post(url: URL, headers: Headers, body: RawBody) = PostRequest(url, headers, body) {
    IO {
      val req = request(SprayHttpMethods.POST, url, headers, body)
      val resp = (AkkaIO(Http) ? req).mapTo[SprayHttpResponse]
      resp.executeToNewmanPromise(req, defaultContentType)
    }
  }

  override def put(url: URL, headers: Headers, body: RawBody) = PutRequest(url, headers, body) {
    IO {
      val req = request(SprayHttpMethods.PUT, url, headers, body)
      val resp = (AkkaIO(Http) ? req).mapTo[SprayHttpResponse]
      resp.executeToNewmanPromise(req, defaultContentType)
    }
  }

  override def delete(url: URL, headers: Headers) = DeleteRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.DELETE, url, headers)
      val resp = (AkkaIO(Http) ? req).mapTo[SprayHttpResponse]
      resp.executeToNewmanPromise(req, defaultContentType)
    }
  }

  override def head(url: URL, headers: Headers) = HeadRequest(url, headers) {
    IO {
      val req = request(SprayHttpMethods.HEAD, url, headers)
      val resp = (AkkaIO(Http) ? req).mapTo[SprayHttpResponse]
      resp.executeToNewmanPromise(req, defaultContentType)
    }
  }

}

object SprayHttpClient {
  private[SprayHttpClient] lazy val DefaultActorSystem = ActorSystem()

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
          case mainType :: subType :: Nil => SprayMediaType.custom(mainType, subType)
          case mainType :: Nil => SprayMediaType.custom(mainType, "")
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
        code <- HttpResponseCode.fromInt(resp.status.intValue)
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

  private[SprayHttpClient] implicit class RichPipeline(pipeline: Future[SprayHttpResponse]) {
    def executeToNewmanPromise(req: SprayHttpRequest,
                               defaultContentType: SprayContentType): Promise[HttpResponse] = {
      pipeline.map { res =>
        res.toNewmanHttpResponse(defaultContentType) | (throw new InvalidSprayResponse(res.status.intValue))
      }.toScalazPromise
    }
  }

  private[SprayHttpClient] class InvalidSprayResponse(code: Int) extends Exception(s"Invalid spray HTTP response with code $code")
}
