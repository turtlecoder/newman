package com.stackmob.newman

import net.liftweb.json.scalaz.JsonScalaz._
/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.jsonscalaz
 *
 * User: aaron
 * Date: 12/27/12
 * Time: 10:17 PM
 */
package object jsonscalaz {
  trait ErrorW {
    protected def error: Error

    def fold[T](unexpected: UnexpectedJSONError => T,
                noSuchField: NoSuchFieldError => T,
                uncategorized: UncategorizedError => T) = error match {
      case u@UnexpectedJSONError(_, _) => unexpected(u)
      case n@NoSuchFieldError(_, _) => noSuchField(n)
      case u@UncategorizedError(_, _, _) => uncategorized(u)
    }
  }

  implicit def errorToW(e: Error) = new ErrorW {
    override lazy val error = e
  }

}
