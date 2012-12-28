package com.stackmob

import scalaz._
import Scalaz._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 12/27/12
 * Time: 9:54 PM
 */
package object newman {
  type ThrowableValidation[T] = Validation[Throwable, T]

  sealed trait ValidationW[Fail, Success] {
    def v: Validation[Fail, Success]

    def mapFailure[NewFail](transform: Fail => NewFail): Validation[NewFail, Success] = v match {
      case Success(s) => s.success[NewFail]
      case Failure(f) => transform(f).fail[Success]
    }
  }
  implicit def validationToW[T, U](validation: Validation[T, U]) = new ValidationW[T, U] {
    override lazy val v = validation
  }

  def validating[T](t: => T): ThrowableValidation[T] = try {
    t.success[Throwable]
  } catch {
    case e: Throwable => e.fail[T]
  }
}
