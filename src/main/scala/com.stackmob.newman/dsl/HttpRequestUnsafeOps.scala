package com.stackmob.newman.dsl

import scalaz.Validation
import com.stackmob.common.validation._
import com.stackmob.newman._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.dsl
 *
 * User: aaron
 * Date: 4/27/12
 * Time: 10:12 PM
 */

case class HttpRequestUnsafeOps(h: HttpRequest) {
  @throws(classOf[HttpResponse])
  def unsafeExecute: HttpResponse = h.execute.unsafePerformIO
  def unsafeExecuteAndValidate: Validation[Throwable, HttpResponse] = validating(unsafeExecute)
}

object HttpRequestUnsafeOps {

  implicit def httpRequestToUnsafe(h: HttpRequest) = HttpRequestUnsafeOps(h)

}
