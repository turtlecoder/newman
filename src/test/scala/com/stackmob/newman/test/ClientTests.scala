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
package test

import com.stackmob.newman.{Headers, Constants, HttpClient}
import org.specs2.Specification
import com.stackmob.newman.dsl._
import com.stackmob.newman.response.{HttpResponse, HttpResponseCode}
import java.net.URL
import scalaz.Scalaz._

trait ClientTests { this: Specification with ResponseMatcher =>
  implicit private val charset = Constants.UTF8Charset
  class ClientTests(implicit client: HttpClient) {
    private lazy val DefaultExpectedBodyPieces = List("Host", "httpbin.org")
    private  def execute[T](t: Builder,
                            expectedCode: HttpResponseCode = HttpResponseCode.Ok,
                            expectedHeaders: Headers = Headers("Content-Type" -> "application/json"),
                            mbExpectedBodyPieces: Option[List[String]] = DefaultExpectedBodyPieces.some) = {
      t.block() must beResponse(expectedCode, headers = expectedHeaders, mbBodyPieces = mbExpectedBodyPieces)
    }

    private def executeAsync(t: Builder,
                             expectedCode: HttpResponseCode = HttpResponseCode.Ok,
                             expectedHeaders: Headers = None,
                             mbExpectedBodyPieces: Option[List[String]] = DefaultExpectedBodyPieces.some) = {
      import com.stackmob.newman.concurrent.SequentialExecutionContext
      val responseFuture = t.apply.map { resp: HttpResponse =>
        resp must beResponse(expectedCode, headers = expectedHeaders, mbBodyPieces = mbExpectedBodyPieces)
      }
      responseFuture.block()
    }

    private lazy val headerTup = "X-Stackmob-Test-Header" -> "X-StackMob-Test-Value"
    //part of the JSON body should contain the header, in this format
    private lazy val headers = Headers(headerTup)

    private lazy val getURL = new URL("http://httpbin.org/get")
    private lazy val postURL = new URL("http://httpbin.org/post")
    private lazy val putURL = new URL("http://httpbin.org/put")
    private lazy val deleteURL = new URL("http://httpbin.org/delete")
    private lazy val headURL = new URL("http://httpbin.org/get")

    private lazy val bodyString = "StackMobTestBody"
    private lazy val body = bodyString.getBytes(charset)

    private lazy val getBuilder = GET(getURL).addHeaders(headers)
    def get = {
      execute(getBuilder, mbExpectedBodyPieces = (headerTup._1 :: headerTup._2 :: DefaultExpectedBodyPieces).some)
    }
    def getAsync = {
      executeAsync(getBuilder, mbExpectedBodyPieces = (headerTup._1 :: headerTup._2 :: DefaultExpectedBodyPieces).some)
    }

    private val postBuilder = POST(postURL).addBody(body).addHeaders(headers)
    def post = {
      execute(postBuilder, mbExpectedBodyPieces = (headerTup._1 :: headerTup._2 :: "data" :: bodyString :: DefaultExpectedBodyPieces).some)
    }
    def postAsync = {
      executeAsync(postBuilder, mbExpectedBodyPieces = (headerTup._1 :: headerTup._2 :: "data" :: bodyString :: DefaultExpectedBodyPieces).some)
    }

    private val putBuilder = PUT(putURL).addBody(body).addHeaders(headers)
    def put = {
      execute(putBuilder, mbExpectedBodyPieces = (headerTup._1 :: headerTup._2 :: "data" :: bodyString :: DefaultExpectedBodyPieces).some)
    }
    def putAsync = {
      executeAsync(putBuilder, mbExpectedBodyPieces = (headerTup._1 :: headerTup._2 :: "data" :: bodyString :: DefaultExpectedBodyPieces).some)
    }

    private val deleteBuilder = DELETE(deleteURL).addHeaders(headers)
    def delete = {
      execute(deleteBuilder, mbExpectedBodyPieces = (headerTup._1 :: headerTup._2 :: DefaultExpectedBodyPieces).some)
    }
    def deleteAsync = {
      executeAsync(deleteBuilder, mbExpectedBodyPieces = (headerTup._1 :: headerTup._2 :: DefaultExpectedBodyPieces).some)
    }

    private val headBuilder = HEAD(headURL).addHeaders(headers)
    def head = {
      execute(headBuilder, mbExpectedBodyPieces = None)
    }
    def headAsync = {
      executeAsync(headBuilder, mbExpectedBodyPieces = None)
    }
  }

  object ClientTests {
    def apply(client: HttpClient) = {
      new ClientTests()(client)
    }
  }

}

