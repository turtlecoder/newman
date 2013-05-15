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

package com.stackmob.newman.test

import com.stackmob.newman.{Headers, Constants, HttpClient}
import org.specs2.Specification
import com.stackmob.newman.dsl._
import com.stackmob.newman.response.{HttpResponse, HttpResponseCode}
import java.net.URL
import scalaz.Scalaz._

trait ClientTests { this: Specification with ResponseMatcher =>
  implicit private val charset = Constants.UTF8Charset

  private implicit class RichStringTuple(tup: (String, String)) {
    /**
     * calculates the string representation of a request header, in the JSON body
     * @return the string representation
     */
    def headerString = {
      """"%s": "%s"""".format(tup._1, tup._2)
    }
  }

  class ClientTests(implicit client: HttpClient) {
    private lazy val DefaultExpectedBody: String = ("Host" -> "httpbin.org").headerString
    private  def execute[T](t: Builder,
                            expectedCode: HttpResponseCode = HttpResponseCode.Ok,
                            expectedHeaders: Headers = None,
                            mbExpectedBodyPieces: Option[List[String]] = List(DefaultExpectedBody).some) = {
      val r = t.executeUnsafe
      r must beResponse(expectedCode, headers = expectedHeaders, mbBodyPieces = mbExpectedBodyPieces)
    }

    private def executeAsync(t: Builder,
                             expectedCode: HttpResponseCode = HttpResponseCode.Ok,
                             expectedHeaders: Headers = None,
                             mbExpectedBodyPieces: Option[List[String]] = List(DefaultExpectedBody).some) = {
      val rPromise = t.executeAsyncUnsafe
      rPromise.map { r: HttpResponse =>
        r must beResponse(expectedCode, headers = expectedHeaders, mbBodyPieces = mbExpectedBodyPieces)
      }.get
    }

    private lazy val headerTup = "X-StackMob-Test-Header" -> "X-StackMob-Test-Value"
    //part of the JSON body should contain the header, in this format
    private lazy val headerBody = headerTup.headerString
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
      execute(getBuilder, mbExpectedBodyPieces = List(DefaultExpectedBody, headerBody).some)
    }
    def getAsync = {
      executeAsync(getBuilder)
    }

    private val postBuilder = POST(postURL).addBody(body).addHeaders(headers)
    def post = {
      execute(postBuilder, mbExpectedBodyPieces = List(bodyString).some)
    }
    def postAsync = {
      executeAsync(postBuilder, mbExpectedBodyPieces = List(bodyString).some)
    }

    private val putBuilder = PUT(putURL).addBody(body).addHeaders(headers)
    def put = {
      execute(putBuilder, mbExpectedBodyPieces = List(bodyString).some)
    }
    def putAsync = {
      executeAsync(putBuilder, mbExpectedBodyPieces = List(bodyString).some)
    }

    private val deleteBuilder = DELETE(deleteURL).addHeaders(headers)
    def delete = {
      execute(deleteBuilder)
    }
    def deleteAsync = {
      executeAsync(deleteBuilder)
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

