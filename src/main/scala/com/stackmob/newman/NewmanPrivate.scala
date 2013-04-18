/**
 * Copyright 2013 StackMob
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

import scalaz._
import Scalaz._
import scalaz.Failure
import scalaz.Success

trait NewmanPrivate {

  implicit class Identity[Id](value: Id) {
    def cast[T](implicit target: Manifest[T]): Option[T] = {
      Option(value).flatMap { _ =>
        val source = value match {
          case _: Boolean => manifest[Boolean]
          case _: Byte => manifest[Byte]
          case _: Char => manifest[Char]
          case _: Short => manifest[Short]
          case _: Int => manifest[Int]
          case _: Long => manifest[Long]
          case _: Float => manifest[Float]
          case _: Double => manifest[Double]
          case _ => Manifest.classType(value.getClass)
        }
        (target.runtimeClass.isAssignableFrom(source.runtimeClass)).option(value.asInstanceOf[T])
      }
    }
  }

  private[newman] type ThrowableValidation[T] = Validation[Throwable, T]

  implicit class RichValidation[Fail, Success](v: Validation[Fail, Success]) {
    def mapFailure[NewFail](transform: Fail => NewFail): Validation[NewFail, Success] = v match {
      case Success(s) => s.success[NewFail]
      case Failure(f) => transform(f).fail[Success]
    }
  }

}
