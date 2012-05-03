package com.stackmob.newman

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman
 *
 * User: aaron
 * Date: 4/27/12
 * Time: 10:14 PM
 */

object Exceptions {
  case class UnknownHttpStatusCodeException(i: Int) extends Exception("Unknown HTTP status code %d".format(i))
}
