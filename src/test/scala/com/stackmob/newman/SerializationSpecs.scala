package com.stackmob.newman

import com.stackmob.newman.DSL._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import java.net.URL
import com.stackmob.newman.request.HttpRequest._
import response.{HttpResponseCode, HttpResponse}
import scalaz._
import Scalaz._
import net.liftweb.json._
import org.specs2.matcher.Matcher
import net.liftweb.json.scalaz.JsonScalaz._


/**
 * Created by IntelliJ IDEA.
 *
 *
 *
 * User: Kelsey
 * Date: 5/21/12
 * Time: 3:30 PM
 */

class SerializationSpecs extends Specification { def is =
  "SerializationSpecs".title                                                                                                      ^
  """
  The Newman DSL is intended to make it easy to construct and execute HTTP requests
  """                                                                                                                   ^
  "Serialization should"                                                                                                ^
      "correctly replace a body"                                                                                        ! SerializationTest().correctlySetsBody ^
    "Deerialization should"                                                                                             ^
        "deserialize with a provided JSONR"                                                                             ! DeserializationTest().deserializesWithJSONR ^
        "deserialize without a provided JSONR"                                                                          ! DeserializationTest().deserializesWithoutJSONR ^
        "deserialize with an overriding JSONR"                                                                          ! DeserializationTest().deserializesWithSpecificJSONR ^
//        "successfully deserialize against a partial function"                                                           ! DeserializationTest().successfullyDeserializesOnPartialFunction ^
                                                                                                                        end
  protected val url = new URL("http://stackmob.com")

  trait Context extends BaseContext {
    implicit protected val client = new DummyHttpClient
    protected def ensureEmptyHeaders[T <: Builder](t: T)(implicit m: Manifest[T]): SpecsResult = {
      (t must beAnInstanceOf[T]) and
      (t.headers must beEqualTo(Headers.empty))
    }

    protected def transformer: Builder

    def succeedWith[E, A](a: =>A) = validationWith[E, A](Success(a))

    private def validationWith[E, A](f: =>Validation[E, A]): Matcher[Validation[E, A]] = (v: Validation[E, A]) => {
        val expected = f
        (expected == v, v+" is a "+expected, v+" is not a "+expected)
     }

  }


  case class SerializationTest() extends Context {
    override protected val transformer = PUT(url)

    def correctlySetsBody: SpecsResult = {
      import net.liftweb.json.scalaz.JsonScalaz.fromJSON
      val bcc = SomeClass("all", 4, "u")
      val resultantBody: Array[Byte] = transformer.setBody(bcc).body
      fromJSON[SomeClass](parse(new String(resultantBody))) must succeedWith(bcc)
    }
  }

  case class DeserializationTest() extends Context {
    override protected val transformer = GET(url)

    def deserializesWithJSONR: SpecsResult = {
      import net.liftweb.json.scalaz.JsonScalaz.toJSON
      val bodyObject = SomeClass("boyz", 2, "men")
      client.responseToReturn = HttpResponse(HttpResponseCode.Ok, Headers.empty, compact(render(toJSON(bodyObject))).getBytes(com.stackmob.newman.Constants.UTF8Charset))
      transformer.executeUnsafe.as[SomeClass].body must succeedWith(bodyObject)
    }

    def deserializesWithoutJSONR: SpecsResult = {
      import net.liftweb.json.scalaz.JsonScalaz.toJSON
      val bodyObject = AnotherClass(9.5, false)
      client.responseToReturn = HttpResponse(HttpResponseCode.Ok, Headers.empty, compact(render(toJSON(bodyObject))).getBytes(com.stackmob.newman.Constants.UTF8Charset))
      transformer.executeUnsafe.as[AnotherClass].body must succeedWith(bodyObject)
    }

    //    def successfullyDeserializesOnPartialFunction: SpecsResult = {
    //      import net.liftweb.json.scalaz.JsonScalaz.toJSON
    //      val bodyObject = SomeClass("me", 2, "u")
    //      val notTheBodyObject = AnotherClass(9.5, false)
    //      client.responseToReturn = HttpResponse(HttpResponseCode.Ok, Headers.empty, compact(render(toJSON(bodyObject))).getBytes(com.stackmob.newman.Constants.UTF8Charset))
    //      val pfResult = transformer.executeUnsafe{
    //        case HttpResponseCode.Ok => classOf[SomeClass]
    //        case HttpResponseCode.InternalServerError => classOf[AnotherClass]
    //      }
    //      pfResult must beEqualTo TypedHttpResponse(HttpResponseCode.Ok, Headers.empty, bodyObject)
    //    }

    def deserializesWithSpecificJSONR: SpecsResult = {
      import net.liftweb.json.scalaz.JsonScalaz.toJSON
      val bodyObject = SomeClass("boyz", 2, "men")
      client.responseToReturn = HttpResponse(HttpResponseCode.Ok, Headers.empty, compact(render(toJSON(bodyObject))).getBytes(com.stackmob.newman.Constants.UTF8Charset))

      case class justStrings(a: String, c: String)
      implicit def reader: JSONR[justStrings] =
          new JSONR[justStrings] {
            def read(json: JValue): Result[justStrings] = {
              (field[String]("first")(json) |@|
                field[String]("second")(json)) {
                justStrings(_, _)
              }
            }
          }
      transformer.executeUnsafe.as[justStrings].body must succeedWith(justStrings("boyz", "men"))
    }
  }

}

case class SomeClass(a: String, b: Int, c: String)

object SomeClass {
  implicit def reader: JSONR[SomeClass] =
    new JSONR[SomeClass] {
      def read(json: JValue): Result[SomeClass] = {
        (field[String]("a")(json) |@|
          field[Int]("b")(json) |@|
          field[String]("c")(json)) {
          SomeClass(_, _, _)
        }
      }
    }

  implicit def writer: JSONW[SomeClass] =
    new JSONW[SomeClass] {
      def write(obj: SomeClass): JValue = {
        JObject(
          JField("a", JString(obj.a)) ::
            JField("b", JInt(obj.b)) ::
            JField("c", JString(obj.c)) ::
            Nil)
      }
    }
}

case class AnotherClass(x: Double, y: Boolean)

object AnotherClass {
  implicit def writer: JSONW[AnotherClass] =
    new JSONW[AnotherClass] {
      def write(obj: AnotherClass): JValue = {
        JObject(
          JField("x", JDouble(obj.x)) ::
            JField("y", JBool(obj.y)) ::
            Nil)
      }
    }
}