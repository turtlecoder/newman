package com.stackmob.newman

import com.stackmob.newman.DSL._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import java.net.URL
import com.stackmob.newman.request.HttpRequest._
import request.HttpRequestWithBody._
import request.{HeadRequest, PutRequest, GetRequest}
import scalaz._
import Scalaz._

/**
 * Created by IntelliJ IDEA.
 *
 *
 *
 * User: aaron
 * Date: 5/10/12
 * Time: 3:30 PM
 */

class DSLSpecs extends Specification { def is =
  "DSLSpecs".title                                                                                                      ^
  """
  The Newman DSL is intended to make it easy to construct and execute HTTP requests
  """                                                                                                                   ^
  "GET should"                                                                                                          ^
    "return Headers => GetRequest"                                                                                      ! GetTest().returnsProperFunction ^
                                                                                                                        end ^
  "POST should"                                                                                                         ^
    "return (Headers, RawBody) => PostRequest"                                                                          ! PostTest().returnsProperFunction ^
                                                                                                                        end ^
  "PUT should"                                                                                                          ^
    "return (Headers, RawBody) => PutRequest"                                                                           ! PutTest().returnsProperFunction ^
                                                                                                                        end ^
  "DELETE should"                                                                                                       ^
    "return Headers => DeleteRequest"                                                                                   ! DeleteTest().returnsProperFunction ^
                                                                                                                        end ^
  "HEAD should"                                                                                                         ^
    "return Headers => HeadRequest"                                                                                     ! HeadTest().returnsProperFunction ^
                                                                                                                        end ^
  "HeaderTransformer should"                                                                                            ^
    "correctly add a header"                                                                                            ! HeaderTransformerTest().correctlyAddsAHeader ^
    "correctly add headers"                                                                                             ! HeaderTransformerTest().correctlyAddsHeaders ^
    "correctly prepend headers"                                                                                         ! HeaderTransformerTest().correctlyPrependsHeaders ^
                                                                                                                        end

  trait Context extends BaseContext {
    implicit protected val client = new DummyHttpClient
    protected val url = new URL("http://stackmob.com")
  }

  case class GetTest() extends Context {
    def returnsProperFunction: SpecsResult = GET(url) must beAnInstanceOf[HeaderTransformer]
  }

  case class PostTest() extends Context {
    def returnsProperFunction: SpecsResult = POST(url) must beAnInstanceOf[HeaderAndBodyTransformer]
  }

  case class PutTest() extends Context {
    def returnsProperFunction: SpecsResult = PUT(url) must beAnInstanceOf[HeaderAndBodyTransformer]
  }

  case class DeleteTest() extends Context {
    def returnsProperFunction: SpecsResult = DELETE(url) must beAnInstanceOf[HeaderTransformer]
  }

  case class HeadTest() extends Context {
    def returnsProperFunction: SpecsResult = HEAD(url) must beAnInstanceOf[HeaderTransformer]
  }

  case class HeaderTransformerTest() extends Context {
    private val header1 = "testHeaderName" -> "testHeaderVal"
    private val header2 = "testHeaderName2" -> "testHeaderVal2"
    private val headers = nel(header1, header2)
    private val transformer = GET(url)

    def correctlyAddsAHeader: SpecsResult = transformer.addHeader(header1).soFar must beEqualTo(Some(nel(header1)))
    def correctlyAddsHeaders: SpecsResult = transformer.addHeaders(headers.some).soFar must beEqualTo(Some(headers))
    def correctlyPrependsHeaders: SpecsResult = {
      val oneAtATime = transformer.addHeader(header1).addHeader(header2).soFar must beEqualTo(Some(nel(header2, header1)))
      val multipleAtATime = transformer.addHeaders(headers.some).addHeader(header2).soFar must beEqualTo(Some(nel(header2, headers.list)))
      oneAtATime and multipleAtATime
    }
  }
}
