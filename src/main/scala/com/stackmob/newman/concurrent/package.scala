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
import scala.concurrent.{Future => ScalaFuture, Promise, ExecutionContext}
import java.util.{Timer, TimerTask}
import scala.concurrent.duration._

package object concurrent {

  implicit val SequentialExecutionContext: ExecutionContext = new ExecutionContext {
    def execute(runnable: Runnable) {
      runnable.run()
    }
    def reportFailure(t: Throwable) {}
    override lazy val prepare: ExecutionContext = this
  }

  implicit class RichTwitterFuture[T](future: TwitterFuture[T]) {
    def toScalaFuture: ScalaFuture[T] = {
      val promise = Promise[T]()
      future.onSuccess { result =>
        promise.success(result)
      }.onFailure { throwable =>
        promise.failure(throwable)
      }
      promise.future
    }
  }

  implicit class RichJavaTimer(timer: Timer) {
    def schedule(duration: FiniteDuration)(fn: => Unit) {
      val timerTask = new TimerTask {
        def run() {
          fn
        }
      }
      timer.schedule(timerTask, duration.toMillis)
    }
  }
}
