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

  private val verbs = Map(
    Get.stringVal.toLowerCase -> Get,
    Post.stringVal.toLowerCase -> Post,
    Put.stringVal.toLowerCase -> Put,
    Delete.stringVal.toLowerCase -> Delete,
    Head.stringVal.toLowerCase -> Head
  )

  implicit def HttpVerbToReader: EnumReader[HttpVerb] = new EnumReader[HttpVerb] {
    def read(s: String): Option[HttpVerb] = verbs.get(s.toLowerCase)
  }
}
