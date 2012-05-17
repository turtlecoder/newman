package com.stackmob.newman.caching

import scalaz.effects._
import com.stackmob.newman.response.HttpResponse
import com.stackmob.newman.request.HttpRequest
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.caching
 *
 * User: aaron
 * Date: 5/16/12
 * Time: 4:30 PM
 */

class DummyHttpResponseCacher(onGet: => Option[HttpResponse],
                              onSet: => Unit,
                              onExists: => Boolean) extends HttpResponseCacher {

  val getCalls = new CopyOnWriteArrayList[HttpRequest]()
  val setCalls = new CopyOnWriteArrayList[(HttpRequest, HttpResponse)]()
  val existsCalls = new CopyOnWriteArrayList[HttpRequest]()
  def totalNumCalls = getCalls.size() + setCalls.size() + existsCalls.size()

  override def get(req: HttpRequest) = io {
    getCalls.add(req)
    onGet
  }

  override def set(req: HttpRequest, resp: HttpResponse) = io {
    setCalls.add(req -> resp)
    onSet
  }

  override def exists(req: HttpRequest) = io {
    existsCalls.add(req)
    onExists
  }
}
