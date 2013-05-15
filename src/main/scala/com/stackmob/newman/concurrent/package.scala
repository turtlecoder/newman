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

import com.twitter.util.{Future => TwitterFuture}
import scalaz.concurrent.{Strategy => ScalazStrategy, Promise => ScalazPromise}
import scala.concurrent.{Future => ScalaFuture, ExecutionContext}

package object concurrent {

  implicit val SequentialExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable) {
      runnable.run()
    }
    def reportFailure(t: Throwable) {}
    override lazy val prepare: ExecutionContext = this
  }

  implicit class RichTwitterFuture[T](future: TwitterFuture[T]) {
    def toScalazPromise: ScalazPromise[T] = {
      val promise = ScalazPromise.emptyPromise[T](ScalazStrategy.Sequential)
      future.onSuccess { result =>
        promise.fulfill(result)
      }.onFailure { throwable =>
        promise.fulfill(throw throwable)
      }
      promise
    }
  }

  implicit class RichScalaFuture[T](fut: ScalaFuture[T]) {
    def toScalazPromise: ScalazPromise[T] = {
      val promise = ScalazPromise.emptyPromise[T](ScalazStrategy.Sequential)
      fut.map { result =>
        promise.fulfill(result)
      }.onFailure {
        case t: Throwable => promise.fulfill(throw t)
      }
      promise
    }
  }
}
