package com.stackmob.newman.caching

import com.stackmob.newman.response.HttpResponse
import com.stackmob.newman.request.HttpRequest
import scalaz.effects._
import scalaz.concurrent._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.caching
 *
 * User: aaron
 * Date: 5/15/12
 * Time: 4:23 PM
 */

trait HttpResponseCacher {
  def get(req: HttpRequest): IO[Option[HttpResponse]]
  def set(req: HttpRequest, resp: HttpResponse): IO[Unit]
  def exists(req: HttpRequest): IO[Boolean]
}
