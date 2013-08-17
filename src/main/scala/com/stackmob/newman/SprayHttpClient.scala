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
  HttpHeaders => SprayHttpHeaders,
  HttpRequest => SprayHttpRequest,
  HttpResponse => SprayHttpResponse,
  HttpMethod => SprayHttpMethod,
  HttpMethods => SprayHttpMethods,
  HttpBody => SprayHttpBody,
  ContentTypes => SprayContentTypes,
  ContentType => SprayContentType,
  HttpEntity => SprayHttpEntity,
  EmptyEntity => SprayEmptyEntity}
import spray.http.HttpHeaders.RawHeader
import java.net.URL
import com.stackmob.newman.request._
import com.stackmob.newman.response._
import scalaz.effect.IO
import scala.concurrent.Future
import scalaz.Scalaz._
import com.stackmob.newman.response.HttpResponse
import scalaz.NonEmptyList
import akka.io.{IO => AkkaIO}
import akka.pattern.ask
import spray.can.Http
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import com.stackmob.newman.concurrent.SequentialExecutionContext
import com.stackmob.newman.Exceptions.InternalException
import spray.http.parser.HttpParser

class SprayHttpClient(actorSystem: ActorSystem = SprayHttpClient.DefaultActorSystem,
                      defaultContentType: SprayContentType = SprayContentTypes.`application/json`,
                      timeout: Timeout = Timeout(5, TimeUnit.SECONDS)) extends HttpClient {

  import SprayHttpClient._


  private implicit val clientActorSystem: ActorSystem = actorSystem
  private implicit val clientTimeout: Timeout = timeout

  private def perform(method: SprayHttpMethod,
                      url: URL,
                      headers: Headers,
                      rawBody: RawBody = RawBody.empty): IO[Future[HttpResponse]] = {
    IO {
      val resp = (AkkaIO(Http) ? request(method, url, headers, rawBody)).mapTo[SprayHttpResponse]
      resp.executeToNewmanPromise(defaultContentType)
    } except {
      case c: ClassCastException => throw InternalException("Unexpected return type", c.some)
      case t: Throwable => throw t
    }
  }

  private def request(method: SprayHttpMethod,
                      url: URL,
                      headers: Headers,
                      rawBody: RawBody): SprayHttpRequest = {
    val headerList = headers.map { headerNel =>
      val lst = headerNel.list
      lst.map { hdr =>
        RawHeader(hdr._1, hdr._2)
      }
    } | Nil

    val entity: SprayHttpEntity = {
      if (rawBody.length === 0) {
        SprayEmptyEntity
      } else {
        val contentType = headers.getContentType(defaultContentType)
        SprayHttpEntity(contentType, rawBody)
      }
    }

    SprayHttpRequest(method, Uri(url.toString), headerList, entity)
  }

  override def get(url: URL, headers: Headers): GetRequest = {
    GetRequest(url, headers) {
      perform(SprayHttpMethods.GET, url, headers)
    }
  }

  override def post(url: URL, headers: Headers, body: RawBody): PostRequest = {
    PostRequest(url, headers, body) {
      perform(SprayHttpMethods.POST, url, headers, body)
    }
  }

  override def put(url: URL, headers: Headers, body: RawBody): PutRequest = {
    PutRequest(url, headers, body) {
      perform(SprayHttpMethods.PUT, url, headers, body)
    }
  }

  override def delete(url: URL, headers: Headers): DeleteRequest = {
    DeleteRequest(url, headers) {
      perform(SprayHttpMethods.DELETE, url, headers)
    }
  }

  override def head(url: URL, headers: Headers): HeadRequest = {
    HeadRequest(url, headers) {
      perform(SprayHttpMethods.HEAD, url, headers)
    }
  }

}

object SprayHttpClient {

  private[SprayHttpClient] lazy val DefaultActorSystem = ActorSystem()

  implicit class RichHeaders(headers: Headers) {
    def getContentType(defaultContentType: SprayContentType): SprayContentType = {
      headers.flatMap { lst: HeaderList =>
        val results = lst.list.map(x => HttpParser.parseHeader(RawHeader(x._1, x._2))).collect({ case Right(c) => c })
        results.collect({ case c @ SprayHttpHeaders.`Content-Type`(_) => c }).map(_.contentType).headOption
      } | defaultContentType
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
    def executeToNewmanPromise(defaultContentType: SprayContentType): Future[HttpResponse] = {
      pipeline.map { res =>
        res.toNewmanHttpResponse(defaultContentType) | (throw new InvalidSprayResponse(res.status.intValue))
      }
    }
  }

  private[SprayHttpClient] class InvalidSprayResponse(code: Int) extends Exception(s"Invalid spray HTTP response with code $code")

}
