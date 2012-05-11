package com.stackmob.newman

import com.stackmob.newman.DSL._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import java.net.URL
import com.stackmob.newman.request.HttpRequest._
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
    "return a HeaderTransformer"                                                                                        ! GetTest().returnsProperFunction ^
                                                                                                                        end ^
  "POST should"                                                                                                         ^
    "return a HeaderAndBodyTransformer"                                                                                 ! PostTest().returnsProperFunction ^
                                                                                                                        end ^
  "PUT should"                                                                                                          ^
    "return a HeaderAndBodyTransformer"                                                                                 ! PutTest().returnsProperFunction ^
                                                                                                                        end ^
  "DELETE should"                                                                                                       ^
    "return a HeaderTransformer"                                                                                        ! DeleteTest().returnsProperFunction ^
                                                                                                                        end ^
  "HEAD should"                                                                                                         ^
    "return a HeaderTransformer"                                                                                        ! HeadTest().returnsProperFunction ^
    "execute a HEAD request"                                                                                            ! HeadTest().executesCorrectly ^
                                                                                                                        end ^
  "HeaderTransformer should"                                                                                            ^
    "correctly add a header"                                                                                            ! HeaderTransformerTest().correctlyAddsAHeader ^
    "correctly add headers"                                                                                             ! HeaderTransformerTest().correctlyAddsHeaders ^
    "correctly prepend headers"                                                                                         ! HeaderTransformerTest().correctlyPrependsHeaders ^
                                                                                                                        end ^
  "HeaderAndBodyTransformer should"                                                                                     ^
    "correctly add a header"                                                                                            ! HeaderAndBodyTransformerTest().correctlyAddsAHeader ^
    "correctly add headers"                                                                                             ! HeaderAndBodyTransformerTest().correctlyAddsHeaders ^
    "correctly prepend headers"                                                                                         ! HeaderAndBodyTransformerTest().correctlyPrependsHeaders ^
    "correctly prepend a body"                                                                                          ! HeaderAndBodyTransformerTest().correctlyPrependsBody ^
                                                                                                                        end
  protected val url = new URL("http://stackmob.com")

  trait Context extends BaseContext {
    implicit protected val client = new DummyHttpClient
    protected def ensureEmptyHeaders[T <: Transformer](t: T)(implicit m: Manifest[T]): SpecsResult = {
      (t must beAnInstanceOf[T]) and
      (t.headers must beNone)
    }
  }

  case class GetTest() extends Context {
    private val t = GET(url)
    def returnsProperFunction: SpecsResult = {
      (t must beAnInstanceOf[HeaderTransformer]) and ensureEmptyHeaders(t)
    }

    def executesCorrectly: SpecsResult = {
      (t.execute.unsafePerformIO must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.getRequests.size() must beEqualTo(1))
    }
  }

  case class PostTest() extends Context {
    val t = POST(url)
    def returnsProperFunction: SpecsResult = {
      (t must beAnInstanceOf[HeaderAndBodyTransformer]) and
      (ensureEmptyHeaders(t)) and
      (t.body.length must beEqualTo(0))
    }

    def executesCorrectly: SpecsResult = {
      (t.execute.unsafePerformIO must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.postRequests.size() must beEqualTo(1))
    }
  }

  case class PutTest() extends Context {
    private val t = PUT(url)
    def returnsProperFunction: SpecsResult = {
      (t must beAnInstanceOf[HeaderAndBodyTransformer]) and
      (ensureEmptyHeaders(PUT(url))) and
      (t.body.length must beEqualTo(0))
    }

    def executesCorrecty: SpecsResult = {
      (t.execute.unsafePerformIO must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.putRequests.size() must beEqualTo(1))
    }
  }

  case class DeleteTest() extends Context {
    private val t = DELETE(url)
    def returnsProperFunction: SpecsResult = {

      (t must beAnInstanceOf[HeaderTransformer]) and ensureEmptyHeaders(t)
    }

    def executesCorrectly: SpecsResult = {
      (t.execute.unsafePerformIO must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.deleteRequests.size must beEqualTo(1))
    }
  }

  case class HeadTest() extends Context {
    private val t = HEAD(url)
    def returnsProperFunction: SpecsResult = {
      (t must beAnInstanceOf[HeaderTransformer]) and ensureEmptyHeaders(t)
    }

    def executesCorrectly: SpecsResult = {
      (t.execute.unsafePerformIO must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.headRequests.size must beEqualTo(1))
    }
  }

  trait HeaderTransformerTestBase extends Context {
    protected def transformer: Transformer

    protected def ensureEqualHeaders(t: Transformer, expected: Headers): SpecsResult = (t.headers must haveTheSameHeadersAs(expected))
    protected def ensureEqualHeaders(t: Transformer, expected: HeaderList): SpecsResult = ensureEqualHeaders(t, Some(expected))
    protected def ensureEqualHeaders(t: Transformer, expected: Header): SpecsResult = ensureEqualHeaders(t, nel(expected))


    protected val header1 = "testHeaderName" -> "testHeaderVal"
    protected val header2 = "testHeaderName2" -> "testHeaderVal2"
    protected val headers = nel(header1, header2)
    def correctlyAddsAHeader = ensureEqualHeaders(transformer.addHeader(header1), header1)
    def correctlyAddsHeaders = ensureEqualHeaders(transformer.addHeaders(headers.some), headers)
    def correctlyPrependsHeaders: SpecsResult = {
      ensureEqualHeaders(transformer.addHeader(header1).addHeader(header2), nel(header2, header1)) and
      ensureEqualHeaders(transformer.addHeaders(headers.some).addHeader(header2), nel(header2, headers.list))
    }
  }

  case class HeaderTransformerTest() extends HeaderTransformerTestBase {
    override protected val transformer = GET(url)
  }

  case class HeaderAndBodyTransformerTest() extends HeaderTransformerTestBase {
    override protected val transformer = POST(url)

    def correctlyPrependsBody: SpecsResult = {
      val b1 = "abc".getBytes
      val b2 = "def".getBytes
      val expected = b1 ++ b2
      val resultantBody: Array[Byte] = transformer.addBody(b2).addBody(b1).body
      resultantBody must beEqualTo(expected)
    }
  }
}
