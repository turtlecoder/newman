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
import com.stackmob.newman.Constants._
import net.liftweb.json.scalaz.JsonScalaz.toJSON



/**
 * Created by IntelliJ IDEA.
 *
 *
 *
 * User: Kelsey
 * Date: 5/21/12
 * Time: 3:30 PM
 */

class BodySerializationSpecs extends Specification { def is =
  "BodySerializationSpecs".title                                                                                                      ^
  """
  The Newman DSL is intended to make it easy to construct and execute HTTP requests
  """                                                                                                                   ^
  "Serialization should"                                                                                                ^
      "serialize with a provided JSONR"                                                                                 ! SerializationTest().serializesWithJSONW ^
      "serialize without a provided JSONR"                                                                              ! SerializationTest().serializesWithoutJSONW ^
      "serialize with a specific JSONR"                                                                                 ! SerializationTest().serializesWithSpecificJSONW ^
   "Deserialization should"                                                                                             ^
      "deserialize with a provided JSONR"                                                                               ! DeserializationTest().deserializesWithJSONR ^
      "deserialize without a provided JSONR"                                                                            ! DeserializationTest().deserializesWithoutJSONR ^
      "deserialize with a specific JSONR"                                                                               ! DeserializationTest().deserializesWithSpecificJSONR ^
      "deserialize with an overriding JSONR"                                                                            ! skipped ^ //DeserializationTest().deserializesWithReplacedJSONR ^
                                                                                                                        end
  protected val url = new URL("http://stackmob.com")

  trait Context extends BaseContext {

    protected def transformer: Builder

    def succeedWith[E, A](a: =>A) = validationWith[E, A](Success(a))

    private def validationWith[E, A](f: =>Validation[E, A]): Matcher[Validation[E, A]] = (v: Validation[E, A]) => {
        val expected = f
        (expected == v, v+" is a "+expected, v+" is not a "+expected)
     }

  }


  case class SerializationTest() extends Context {
    implicit val client = new DummyHttpClient
    override protected val transformer = PUT(url)

    def serializesWithJSONW: SpecsResult = {
      val bodyObject = SomeClass("all", 4, "u")
      val resultantBody: Array[Byte] = transformer.setBody(bodyObject).body
      fromJSON[SomeClass](parse(new String(resultantBody))) must succeedWith(bodyObject)
    }

    def serializesWithoutJSONW: SpecsResult = {
      val bodyObject = ClassWithoutWriter(1, 2, "check")
      val resultantBody: String = new String(transformer.setBody(bodyObject).body, UTF8Charset)
      resultantBody must beEqualTo("{\"j\":1,\"k\":2,\"l\":\"check\"}")
    }

    def serializesWithSpecificJSONW: SpecsResult = {

      implicit def stringsWriter: JSONW[SomeClass] =
        new JSONW[SomeClass] {
          def write(obj: SomeClass): JValue = {
            JObject(
              JField("a", JString(obj.a)) ::
                JField("c", JString(obj.c)) ::
                Nil)
          }
        }

      val bodyObject = SomeClass("boyz", 2, "men")
      val resultantBody: String = new String(transformer.setBody(bodyObject).body, UTF8Charset)

      resultantBody must beEqualTo("{\"a\":\"boyz\",\"c\":\"men\"}")
    }
  }

  case class DeserializationTest() extends Context {
    override protected val transformer = GET(url)

    def deserializesWithJSONR: SpecsResult = {
      val bodyObject = SomeClass("boyz", 2, "men")
      val transformer = GET(url)( new DummyHttpClient(HttpResponse(HttpResponseCode.Ok, Headers.empty, compact(render(toJSON(bodyObject))).getBytes(UTF8Charset))))
      transformer.executeUnsafe.as[SomeClass].body must succeedWith(bodyObject)
    }

    def deserializesWithoutJSONR: SpecsResult = {
      val bodyObject = ClassWithoutReader(9.5, false)
      val transformer = GET(url)( new DummyHttpClient(HttpResponse(HttpResponseCode.Ok, Headers.empty, compact(render(toJSON(bodyObject))).getBytes(UTF8Charset))))
      transformer.executeUnsafe.as[ClassWithoutReader].body must succeedWith(bodyObject)
    }

    def deserializesWithSpecificJSONR: SpecsResult = {
      val bodyObject = SomeClass("boyz", 2, "men")
      val transformer = GET(url)( new DummyHttpClient(HttpResponse(HttpResponseCode.Ok, Headers.empty, compact(render(toJSON(bodyObject))).getBytes(UTF8Charset))))

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

    def deserializesWithReplacedJSONR: SpecsResult = {
      val bodyObject = SomeClass("boyz", 2, "men")
      val transformer = GET(url)( new DummyHttpClient(HttpResponse(HttpResponseCode.Ok, Headers.empty, compact(render(toJSON(bodyObject))).getBytes(UTF8Charset))))

      def addAAA(aaa: String, b: Int, c: String): SomeClass = SomeClass("AAA" + aaa, b, c)

      implicit def reader: JSONR[SomeClass] =
        new JSONR[SomeClass] {
          def read(json: JValue): Result[SomeClass] = {
            (field[String]("a")(json) |@|
              field[Int]("b")(json) |@|
              field[String]("c")(json)) {
              addAAA(_, _, _)
            }
          }
        }
      transformer.executeUnsafe.as[SomeClass].body must succeedWith(SomeClass("AAAboyz", 2, "men"))
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

case class ClassWithoutReader(x: Double, y: Boolean)

object ClassWithoutReader {
  implicit def writer: JSONW[ClassWithoutReader] =
    new JSONW[ClassWithoutReader] {
      def write(obj: ClassWithoutReader): JValue = {
        JObject(
          JField("x", JDouble(obj.x)) ::
            JField("y", JBool(obj.y)) ::
            Nil)
      }
    }
}

case class ClassWithoutWriter(j: Int, k: Int, l: String)

object ClassWithoutWriter {
  implicit def reader: JSONR[ClassWithoutWriter] =
    new JSONR[ClassWithoutWriter] {
      def read(json: JValue): Result[ClassWithoutWriter] = {
        (field[Int]("j")(json) |@|
          field[Int]("k")(json) |@|
          field[String]("l")(json)) {
          ClassWithoutWriter(_, _, _)
        }
      }
    }
}