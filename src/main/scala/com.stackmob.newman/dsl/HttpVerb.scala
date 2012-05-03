package com.stackmob.newman.dsl

import com.stackmob.common.enumeration._
import scalaz._
import Scalaz._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.dsl
 *
 * User: aaron
 * Date: 4/27/12
 * Time: 8:25 PM
 */

sealed abstract class HttpVerb(override val stringVal: String) extends Enumeration
sealed abstract class HttpVerbWithBody(override val stringVal: String) extends HttpVerb(stringVal)
sealed abstract class HttpVerbWithoutBody(override val stringVal: String) extends HttpVerb(stringVal)

object HttpVerb {
  object Get extends HttpVerbWithoutBody("GET")
  object Post extends HttpVerbWithBody("POST")
  object Put extends HttpVerbWithBody("PUT")
  object Delete extends HttpVerbWithoutBody("DELETE")
  object Head extends HttpVerbWithoutBody("HEAD")

  implicit def HttpVerbToReader: EnumReader[HttpVerb] = new EnumReader[HttpVerb] {
    def read(s: String): Option[HttpVerb] = s.toLowerCase match {
      case Get.stringVal.toLowerCase => Get.some
      case Post.stringVal.toLowerCase => Post.some
      case Put.stringVal.toLowerCase => Put.some
      case Delete.stringVal.toLowerCase => Delete.some
      case Head.stringVal.toLowerCase => Head.some
      case _ => none
    }
  }
}
