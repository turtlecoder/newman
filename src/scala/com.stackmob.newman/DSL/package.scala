package com.stackmob.newman

import scalaz.Validation
import com.stackmob.common.validation._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.DSL
 *
 * User: aaron
 * Date: 4/27/12
 * Time: 9:04 PM
 */

package object DSL {
  case class HttpRequestUnsafeOps(h: HttpRequest) {
    @throws(classOf[HttpResponse])
    def unsafeExecute: HttpResponse = h.execute.unsafePerformIO
    def unsafeExecuteAndValidate: Validation[Throwable, HttpResponse] = validating(unsafeExecute)
  }
  implicit def httpRequestToUnsafe(h: HttpRequest) = HttpRequestUnsafeOps(h)
}
