package com.stackmob.newman

import com.stackmob.newman.DSL._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import java.net.URL
import com.stackmob.newman.request.HttpRequest._
import scalaz._
import Scalaz._
import net.liftweb.json._
import org.specs2.matcher.Matcher
import org.specs2.matcher.MatchersImplicits


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
    "correctly set a header"                                                                                            ! HeaderTransformerTest().correctlySetsAHeader ^
    "correctly set headers"                                                                                             ! HeaderTransformerTest().correctlySetsHeaders ^
    "correctly replace headers"                                                                                         ! HeaderTransformerTest().correctlyReplacesHeaders ^
                                                                                                                        end ^
  "HeaderAndBodyTransformer should"                                                                                     ^
    "correctly add a header"                                                                                            ! HeaderAndBodyTransformerTest().correctlyAddsAHeader ^
    "correctly add headers"                                                                                             ! HeaderAndBodyTransformerTest().correctlyAddsHeaders ^
    "correctly set a header"                                                                                            ! HeaderAndBodyTransformerTest().correctlySetsAHeader ^
    "correctly set headers"                                                                                             ! HeaderAndBodyTransformerTest().correctlySetsHeaders ^
    "correctly replace headers"                                                                                         ! HeaderAndBodyTransformerTest().correctlyReplacesHeaders ^
    "correctly prepend headers"                                                                                         ! HeaderAndBodyTransformerTest().correctlyPrependsHeaders ^
    "correctly prepend a body"                                                                                          ! HeaderAndBodyTransformerTest().correctlyPrependsBody ^
    "correctly prepend a body"                                                                                          ! HeaderAndBodyTransformerTest().correctlyPrependsBody ^
    "correctly set a body"                                                                                              ! HeaderAndBodyTransformerTest().correctlySetsBody ^
    "correctly set a body when passed a string"                                                                         ! HeaderAndBodyTransformerTest().correctlySetsStringBody ^
    "correctly replace a body"                                                                                          ! HeaderAndBodyTransformerTest().correctlyReplacesBody ^
                                                                                                                        end
  protected val url = new URL("http://stackmob.com")

  trait Context extends BaseContext {
    implicit protected val client = new DummyHttpClient
    protected def ensureEmptyHeaders[T <: Builder](t: T)(implicit m: Manifest[T]): SpecsResult = {
      (t must beAnInstanceOf[T]) and
      (t.headers must beEqualTo(Headers.empty))
    }
  }

  case class GetTest() extends Context {
    private val t = GET(url)
    def returnsProperFunction: SpecsResult = {
      (t must beAnInstanceOf[HeaderBuilder]) and ensureEmptyHeaders(t)
    }

    def executesCorrectly: SpecsResult = {
      (t.executeUnsafe must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.getRequests.size() must beEqualTo(1))
    }
  }

  case class PostTest() extends Context {
    val t = POST(url)
    def returnsProperFunction: SpecsResult = {
      (t must beAnInstanceOf[HeaderAndBodyBuilder]) and
      (ensureEmptyHeaders(t)) and
      (t.body.length must beEqualTo(0))
    }

    def executesCorrectly: SpecsResult = {
      (t.executeUnsafe must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.postRequests.size() must beEqualTo(1))
    }
  }

  case class PutTest() extends Context {
    private val t = PUT(url)
    def returnsProperFunction: SpecsResult = {
      (t must beAnInstanceOf[HeaderAndBodyBuilder]) and
      (ensureEmptyHeaders(PUT(url))) and
      (t.body.length must beEqualTo(0))
    }

    def executesCorrecty: SpecsResult = {
      (t.executeUnsafe must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.putRequests.size() must beEqualTo(1))
    }
  }

  case class DeleteTest() extends Context {
    private val t = DELETE(url)
    def returnsProperFunction: SpecsResult = {

      (t must beAnInstanceOf[HeaderBuilder]) and ensureEmptyHeaders(t)
    }

    def executesCorrectly: SpecsResult = {
      (t.executeUnsafe must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.deleteRequests.size must beEqualTo(1))
    }
  }

  case class HeadTest() extends Context {
    private val t = HEAD(url)
    def returnsProperFunction: SpecsResult = {
      (t must beAnInstanceOf[HeaderBuilder]) and ensureEmptyHeaders(t)
    }

    def executesCorrectly: SpecsResult = {
      (t.executeUnsafe must beEqualTo(DummyHttpClient.CannedResponse)) and
      (client.headRequests.size must beEqualTo(1))
    }
  }

  trait HeaderTransformerTestBase extends Context {
    protected def transformer: Builder

    protected def ensureEqualHeaders(t: Builder, expected: Headers): SpecsResult = (t.headers must haveTheSameHeadersAs(expected))
    protected def ensureEqualHeaders(t: Builder, expected: HeaderList): SpecsResult = ensureEqualHeaders(t, Headers(expected))
    protected def ensureEqualHeaders(t: Builder, expected: Header): SpecsResult = ensureEqualHeaders(t, Headers(expected))


    protected val header1 = "testHeaderName" -> "testHeaderVal"
    protected val header2 = "testHeaderName2" -> "testHeaderVal2"
    protected val headers = nel(header1, header2)
    def correctlyAddsAHeader = ensureEqualHeaders(transformer.addHeaders(header1), header1)
    def correctlyAddsHeaders = ensureEqualHeaders(transformer.addHeaders(headers), headers)
    def correctlyPrependsHeaders: SpecsResult = {
      ensureEqualHeaders(transformer.addHeaders(header1).addHeaders(header2), Headers(header2, header1)) and
      ensureEqualHeaders(transformer.addHeaders(headers).addHeaders(header2), nel(header2, headers.list))
    }
    def correctlySetsAHeader = {
      ensureEqualHeaders(transformer.setHeaders(header1), nel(header1))
    }
    def correctlySetsHeaders = {
      ensureEqualHeaders(transformer.setHeaders(headers), Headers(List(header1, header2)))
    }
    def correctlyReplacesHeaders: SpecsResult = {
      ensureEqualHeaders(transformer.addHeaders(header1).setHeaders(header2), Headers(header2)) and
        ensureEqualHeaders(transformer.addHeaders(headers).setHeaders(header1), nel(header1)) and
        ensureEqualHeaders(transformer.addHeaders(header1).setHeaders(headers), Headers(header1, header2))
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

    def correctlySetsBody: SpecsResult = {
      val b1 = "set".getBytes
      val resultantBody: Array[Byte] = transformer.setBody(b1).body
      resultantBody must beEqualTo(b1)
    }

    def correctlySetsStringBody: SpecsResult = {
      val b1 = "set"
      val resultantBody = transformer.setBody(b1).body
      resultantBody must beEqualTo(b1.getBytes(Constants.UTF8Charset))
    }

    def correctlyReplacesBody: SpecsResult = {
      val b1 = "abc".getBytes
      val b2 = "def".getBytes
      val resultantBody: Array[Byte] = transformer.addBody(b1).setBody(b2).body
      resultantBody must beEqualTo(b2)
    }
  }
}
