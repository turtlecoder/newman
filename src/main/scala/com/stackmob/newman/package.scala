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
  sealed trait ValidationT[F[_], E, A] {
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

    def applyTransform[T[_[_],_]](implicit T: ({type K[X]=ValidationT[F,E,X]})#K~> ({type L[X]=T[F,X]})#L): T[F,A] = T(this)
  }

  def validationT[F[_],E,A](a: F[Validation[E,A]]): ValidationT[F,E,A] = new ValidationT[F,E,A] {
    override lazy val run = a
  }

  def validating[T](t: => T): ThrowableValidation[T] = try {
    t.success[Throwable]
  } catch {
    case e: Throwable => e.fail[T]
  }
}
