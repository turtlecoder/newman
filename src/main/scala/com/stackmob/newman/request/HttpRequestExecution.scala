package com.stackmob.newman.request

import scalaz.concurrent.Promise
import scalaz.effects._
import scalaz._
import Scalaz._
import com.stackmob.newman.response.HttpResponse

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.request
 *
 * User: aaron
 * Date: 5/31/12
 * Time: 7:30 PM
 */

object HttpRequestExecution {
  type RequestResponsePair = (HttpRequest, HttpResponse)
  type RequestResponsePairList = NonEmptyList[RequestResponsePair]
  type RequestPromiseResponsePair = (HttpRequest, Promise[HttpResponse])
  type RequestPromiseResponsePairList = NonEmptyList[RequestPromiseResponsePair]

  /**
   * run a series of requests in sequence (ie: next one begins executing when the previous one completes)
   * @param requests the requests to execute, in order of execution
   * @return an IO representing a list of each request and its response
   */
  def sequencedRequests(requests: NonEmptyList[HttpRequest]): IO[RequestResponsePairList] = {
    val ioList: NonEmptyList[IO[RequestResponsePair]] = requests.map(req => req.pure[IO] <|*|> req.prepare)
    ioList.sequence[IO, RequestResponsePair]
  }

  /**
   * execute a first request, and then execute a series of requests thereafter depending on the previous's response
   * @param firstReq the first request to execute
   * @param remainingRequests the remaining requests to execute, dependant on the previous request's response
   * @return an IO representing a list of each request and its response
   */
  def chainedRequests(firstReq: HttpRequest,
               remainingRequests: NonEmptyList[HttpResponse => HttpRequest]): IO[RequestResponsePairList] = {
    val firstReqIO = firstReq.pure[IO]
    val firstRespIO = firstReq.prepare
    type RunningList = NonEmptyList[IO[(HttpRequest, HttpResponse)]]
    val runningList: RunningList = nel(firstReqIO <|*|> firstRespIO)
    val (_, _, list) = remainingRequests.list.foldLeft((firstReqIO, firstRespIO, runningList)) {
      (running: (IO[HttpRequest], IO[HttpResponse], RunningList), cur: HttpResponse => HttpRequest) =>
        val (_, lastRespIO, runningList) = running
        val newReqIO = lastRespIO.map(cur(_))
        val newRespIO = newReqIO.flatMap(_.prepare)
        val newRunningList = runningList :::> List(newReqIO <|*|> newRespIO)
        (newReqIO, newRespIO, newRunningList)
    }
    list.sequence[IO, RequestResponsePair]
  }

  /**
   * execute a list of requests concurrently
   * @param requests the requests to execute concurrently.
   *                 the ordering of the list does not determine any order or execution, but the ordering of
   *                 (HttpRequest, HttpResponse) pairs will match the ordering of the HttpRequests passed in
   * @return an IO representing a list of each request, and a promise representing its response
   */
  def concurrentRequests(requests: NonEmptyList[HttpRequest]): IO[RequestPromiseResponsePairList] = requests.map { req: HttpRequest =>
    val reqIO: IO[HttpRequest] = req.pure[IO]
    val respIO: IO[Promise[HttpResponse]] = req.prepareAsync
    reqIO <|*|> respIO
  }.sequence[IO, RequestPromiseResponsePair]
}
