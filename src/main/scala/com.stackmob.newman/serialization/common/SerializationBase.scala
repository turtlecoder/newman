package com.stackmob.newman.serialization.common

import net.liftweb.json.scalaz.JsonScalaz._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.serialization.common
 *
 * User: aaron
 * Date: 5/11/12
 * Time: 1:43 PM
 */

trait SerializationBase[T] {
  def writer: JSONW[T]
  def reader: JSONR[T]
}
