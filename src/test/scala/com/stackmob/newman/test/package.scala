package com.stackmob.newman

import scala.concurrent.duration._
import scala.concurrent._
import scalaz.Validation

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.test
 *
 * User: aaron
 * Date: 8/21/13
 * Time: 4:09 PM
 */
package object test {
  private[test] implicit val duration = 250.milliseconds

  private[test] implicit class RichFuture[T](fut: Future[T]) {
    def toEither(dur: Duration = duration): Either[Throwable, T] = {
      Validation.fromTryCatch(block(dur)).toEither
    }

    def block(dur: Duration = duration): T = {
      Await.result(fut, dur)
    }
  }

}
