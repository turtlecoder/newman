package com.stackmob.newman.test

import com.stackmob.newman.{Constants, HttpClient}
import org.specs2.Specification
import com.stackmob.newman.dsl._
import com.stackmob.newman.response.{HttpResponse, HttpResponseCode}
import java.net.URL

trait ClientTests { this: Specification with ResponseMatcher =>

  class ClientTests(implicit client: HttpClient) {
    private  def execute[T](t: Builder,
                            expectedCode: HttpResponseCode = HttpResponseCode.Ok) = {
      val r = t.executeUnsafe
      r must beResponse(expectedCode)
    }

    private def executeAsync(t: Builder,
                             expectedCode: HttpResponseCode = HttpResponseCode.Ok) = {
      val rPromise = t.executeAsyncUnsafe
      rPromise.map { r: HttpResponse =>
        r must beResponse(expectedCode)
      }.get
    }

    private lazy val getURL = new URL("http://httpbin.org/get")
    private lazy val postURL = new URL("http://httpbin.org/post")
    private lazy val putURL = new URL("http://httpbin.org/put")
    private lazy val deleteURL = new URL("http://httpbin.org/delete")
    private lazy val headURL = new URL("http://httpbin.org/get")

    implicit private val encoding = Constants.UTF8Charset

    private lazy val body = "StackmobTestBody".getBytes(encoding)

    private lazy val getBuilder = GET(getURL)
    def get = {
      execute(getBuilder)
    }
    def getAsync = {
      executeAsync(getBuilder)
    }

    private val postBuilder = POST(postURL).addBody(body)
    def post = {
      execute(postBuilder)
    }
    def postAsync = {
      executeAsync(postBuilder)
    }

    private val putBuilder = PUT(putURL).addBody(body)
    def put = {
      execute(putBuilder)
    }
    def putAsync = {
      executeAsync(putBuilder)
    }

    private val deleteBuilder = DELETE(deleteURL)
    def delete = {
      execute(deleteBuilder)
    }
    def deleteAsync = {
      executeAsync(deleteBuilder)
    }

    private val headBuilder = HEAD(headURL)
    def head = {
      execute(headBuilder)
    }
    def headAsync = {
      executeAsync(headBuilder)
    }
  }

  object ClientTests {
    def apply(client: HttpClient) = {
      new ClientTests()(client)
    }
  }

}

