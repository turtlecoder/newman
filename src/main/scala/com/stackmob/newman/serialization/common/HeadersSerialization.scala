package com.stackmob.newman.serialization.common

import com.stackmob.newman.request.HttpRequest._
import scalaz._
import Scalaz._
import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.newman.request.HttpRequest.Headers

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.serialization
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 1:38 PM
 */

object HeadersSerialization extends SerializationBase[Headers] {

  implicit override val writer = new JSONW[Headers] {
    override def write(h: Headers): JValue = {
      val headersList: List[JObject] = h.map {
        headerList: HeaderList =>
          (headerList.list.map { header: Header =>
              JObject(JField("name", JString(header._1)) :: JField("value", JString(header._2)) :: Nil)
          }).toList
      } | List[JObject]()

      JArray(headersList)
    }
  }

  implicit override val reader = new JSONR[Headers] {
    override def read(json: JValue): Result[Headers] = {
      //example incoming AST:
      //JArray(
      //  List(
      //    JObject(
      //      List(
      //        JField(name,JString(header1)),
      //        JField(value,JString(header1))
      //      )
      //    )
      //  )
      //)
      json match {
        case JArray(jObjectList) => {
          val list = jObjectList.flatMap {
            jValue: JValue =>
              jValue match {
                case JObject(jFieldList) => jFieldList match {
                  case JField(_, JString(headerName)) :: JField(_, JString(headerVal)) :: Nil => List(headerName -> headerVal)
                  //TODO: error here
                  case _ => List[(String, String)]()
                }
                //TODO: error here
                case _ => List[(String, String)]()
              }
          }
          val headers: Headers = Headers(list)
          headers.successNel[Error]
        }
        case j => UnexpectedJSONError(j, classOf[JArray]).failNel[Headers]
      }
    }
  }
}
