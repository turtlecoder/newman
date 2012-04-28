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
  type RawBody = Array[Byte]
  val EmptyRawBody = Array[Byte]()

  case class HttpResponse(code: HttpResponseCode, headers: Headers, body: RawBody)
  case class UnknownHttpStatusCodeException(i: Int) extends Exception("Unknown HTTP status code %d".format(i))
}
