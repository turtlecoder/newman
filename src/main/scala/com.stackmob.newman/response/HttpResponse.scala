package com.stackmob.newman.response

import com.stackmob.newman.request._
import HttpRequest._
import HttpRequestWithBody._
import java.nio.charset.Charset

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.response
 *
 * User: aaron
 * Date: 5/2/12
 * Time: 11:54 PM
 */

case class HttpResponse(code: HttpResponseCode, headers: Headers, body: RawBody) {
  def bodyString(charset: Charset = Charset.forName("UTF-8")) = new String(body, charset)
}
