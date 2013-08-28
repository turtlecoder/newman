/**
 * Copyright 2012-2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.newman

import scala.concurrent.duration._
import scala.concurrent._
import scalaz.Validation

package object test {
  private[test] implicit val duration = 2.seconds

  private[test] implicit class RichFuture[T](fut: Future[T]) {
    def toEither(dur: Duration = duration): Either[Throwable, T] = {
      Validation.fromTryCatch(block(dur)).toEither
    }

    def block(dur: Duration = duration): T = {
      Await.result(fut, dur)
    }
  }

}
