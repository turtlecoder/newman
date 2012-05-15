package com.stackmob.newman.request

import com.stackmob.common.enumeration._
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

  implicit val HttpRequestTypeToReader = new EnumReader[HttpRequestType] {
    override def read(s: String) = s.toUpperCase match {
      case GET.stringVal => GET.some
      case POST.stringVal => POST.some
      case PUT.stringVal => PUT.some
      case DELETE.stringVal => DELETE.some
      case HEAD.stringVal => HEAD.some
      case _ => none
    }
  }
}
