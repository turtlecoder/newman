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
  private[newman] trait Identity[Id] {
    def value: Id
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
        (target.erasure.isAssignableFrom(source.erasure)).option(value.asInstanceOf[T])
      }
    }
  }
  private[newman] implicit def tToIdentity[T](t: T): Identity[T] = new Identity[T] {
    override lazy val value = t
  }

  private[newman] type ThrowableValidation[T] = Validation[Throwable, T]

  private[newman] sealed trait ValidationW[Fail, Success] {
    def v: Validation[Fail, Success]

    def mapFailure[NewFail](transform: Fail => NewFail): Validation[NewFail, Success] = v match {
      case Success(s) => s.success[NewFail]
      case Failure(f) => transform(f).fail[Success]
    }
  }
  private[newman] implicit def validationToW[T, U](validation: Validation[T, U]): ValidationW[T, U] = new ValidationW[T, U] {
    override lazy val v = validation
  }

  /*
   * Validation Monad Transformer
   *
   * ValidationT allows you to work inside a F[Validation[E,A]] where F is some monad
   * using the same interface as Validation[E,A]. In fact, given type Id[X] = X,
   * ValidationT[Id,E,A] = Validation[E,A] (Id is the identity monad)
   *
   * When using ValidationT you are treating Validation as a monad, choose this consciously.
   * Errors will not be accumulated like they are as defined in the standard Scalaz type class instances
   * for Validation.
   */
  // TODO: complete interface parity with Validation
  private[newman] sealed trait ValidationT[F[_], E, A] {
    def run: F[Validation[E,A]]

    def map[B](f: A => B)(implicit F: Functor[F]): ValidationT[F,E,B] = {
      validationT(F.fmap(run, (_: Validation[E,A]).map(f)))
    }

    def flatMap[B](f: A => ValidationT[F,E,B])(implicit B: Bind[F], P: Pure[F]): ValidationT[F,E,B] = {
      validationT(B.bind(run, (_: Validation[E,A]).fold(failure = e => P.pure(e.fail[B]), success = a => f(a).run)))
    }

    def flatMapF[B](f: A => F[B])(implicit B: Bind[F], F: Functor[F], P: Pure[F]): ValidationT[F,E,B] = {
      validationT(B.bind(run, (_: Validation[E,A]).fold(
        failure = e => P.pure(e.fail[B]),
        success = v => F.fmap(f(v), (_: B).success[E])
      )))
    }

    def flatMapV[B](f: A => Validation[E,B])(implicit B: Bind[F], P: Pure[F]): ValidationT[F,E,B] = {
      validationT(B.bind(run, (_: Validation[E,A]).fold(
        failure = e => P.pure(e.fail[B]),
        success = v => P.pure(f(v))
      )))
    }

    def getOr(f: E => A)(implicit F: Functor[F]): F[A] =
      F.fmap(run, (_: Validation[E,A]) ||| f)

    def |(default: => A)(implicit F: Functor[F]): F[A] = getOr(_ => default)
  }

  private[newman] def validationT[F[_],E,A](a: F[Validation[E,A]]): ValidationT[F,E,A] = new ValidationT[F,E,A] {
    override lazy val run = a
  }

  private[newman] def validating[T](t: => T): ThrowableValidation[T] = try {
    t.success[Throwable]
  } catch {
    case e: Throwable => e.fail[T]
  }
}
