package com.stackmob

import scalaz._
import Scalaz._

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
  type Header = (String, String)
  type HeaderList = NonEmptyList[Header]
  type Headers = Option[HeaderList]

  case class HttpResponse(code: HttpResponseCode, headers: Headers, body: Array[Byte])
  case class UnknownHttpStatusCodeException(i: Int) extends Exception("Unknown HTTP status code %d".format(i))
}
