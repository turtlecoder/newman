package com.stackmob

import scalaz.NonEmptyList

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 4/27/12
 * Time: 3:47 PM
 */

package object newman {
  type HeaderList = Option[NonEmptyList[(String, String)]]
  case class HttpResponse(code: HttpResponseCode, headers: HeaderList, body: Array[Byte])
}
