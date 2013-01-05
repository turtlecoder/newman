package com.stackmob.newman
package request

import enumeration._
import scalaz._
import Scalaz._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.request
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 3:46 PM
 */

sealed abstract class HttpRequestType(override val stringVal: String) extends Enumeration
object HttpRequestType {
  object GET extends HttpRequestType("GET")
  object POST extends HttpRequestType("POST")
  object PUT extends HttpRequestType("PUT")
  object DELETE extends HttpRequestType("DELETE")
  object HEAD extends HttpRequestType("HEAD")

  implicit val HttpRequestTypeToReader = upperEnumReader(GET, POST, PUT, DELETE, HEAD)
}
