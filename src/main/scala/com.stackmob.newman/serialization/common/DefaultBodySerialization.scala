package com.stackmob.newman.serialization.common

import net.liftweb.json._
import net.liftweb.json.scalaz.JsonScalaz._
import com.stackmob.common.validation._

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.serialization.common
 *
 * User: Kelsey
 * Date: 5/19/12
 * Time: 4:56 PM
 */

object DefaultBodySerialization {

  def getReader[A <: AnyRef](implicit m:Manifest[A]): JSONR[A] = new JSONR[A] {
    override def read(json: JValue) = {

      validating({
        json.extract[A](Serialization.formats(NoTypeHints), m)
        }).mapFailure({ t: Throwable =>
          UncategorizedError(t.getClass.getCanonicalName, t.getMessage, List())
        }).liftFailNel
    }
  }

  //TODO
//  def getWriter[A]: JSONW[A] = new JSONW[A]  {
//    override def write(r: RawBody) = JString(new String(r, UTF8Charset))
//  }
}
