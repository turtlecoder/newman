package com.stackmob.newman.test

import com.stackmob.newman.jsonscalaz._
import org.json4s.scalaz.JsonScalaz._
import com.stackmob.newman.Headers

package object client {
  implicit val expectedResponseJSONR: JSONR[ExpectedResponse] = jsonR { json =>
    for {
      urlString <- field[String]("url")(json)
      args <- field[Map[String, String]]("args")(json)
      headers <- field[Map[String, String]]("headers")(json)
      origin <- field[String]("origin")(json)
      mbData <- field[Option[String]]("data")(json)
    } yield {
      ExpectedResponse(urlString, Headers(headers.toList), args, origin, mbData)
    }
  }
}
