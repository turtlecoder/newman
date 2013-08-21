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

package com.stackmob.newman.request

import com.stackmob.newman.response.HttpResponse
import scala.concurrent._

object HttpRequestExecution {
  type ReqRespFut = (HttpRequest, Future[HttpResponse])

  /**
   * run a series of requests in sequence (ie: next one begins executing when the previous one completes).
   * @param requests the requests to execute, in order of execution
   * @return a list of requests and the futures representing their response.
   *         each represented future after the first will begin executing when the previous future has completed
   */
  def sequencedRequests(requests: List[HttpRequest])
                       (implicit ctx: ExecutionContext): List[ReqRespFut] = {
    val empty = List[(HttpRequest, Future[HttpResponse])]()
    val noLast = Option.empty[Future[HttpResponse]]

    val listAndLastFuture = requests.foldLeft(empty -> noLast) { (tup, req) =>
      val (list, mbLastFuture) = tup
      val thisFuture = mbLastFuture.map { lastFuture =>
        lastFuture.flatMap { _ =>
          req.apply
        }
      }.getOrElse(req.apply)
      (list ++ List(req -> thisFuture)) -> Some(thisFuture)
    }
    listAndLastFuture._1
  }

  /**
   * execute a list of requests concurrently
   * @param requests the requests to execute concurrently.
   *                 the ordering of the list does not determine any order or execution, but the ordering of
   *                 (HttpRequest, HttpResponse) pairs will match the ordering of the HttpRequests passed in
   * @return an IO representing a list of each request, and a promise representing its response
   */
  def concurrentRequests(requests: List[HttpRequest])
                        (implicit ctx: ExecutionContext): Set[(HttpRequest, Future[HttpResponse])] = {
    requests.map { req =>
      req -> req.apply
    }.toSet
  }
}
