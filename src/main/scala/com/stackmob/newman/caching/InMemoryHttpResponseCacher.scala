package com.stackmob.newman.caching

import java.util.concurrent.ConcurrentHashMap
import com.stackmob.newman.response.HttpResponse
import com.stackmob.newman.request.HttpRequest
import scalaz.effects._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.caching
 *
 * User: aaron
 * Date: 5/16/12
 * Time: 5:13 PM
 */

class InMemoryHttpResponseCacher extends HttpResponseCacher {
  private val cache = new ConcurrentHashMap[Array[Byte], HttpResponse]()

  override def get(req: HttpRequest) = io(Option(cache.get(req.hash)))
  override def set(req: HttpRequest, resp: HttpResponse) = io {
    cache.put(req.hash, resp)
    ()
  }

  override def exists(req: HttpRequest) = io(cache.containsKey(req.hash))
}
