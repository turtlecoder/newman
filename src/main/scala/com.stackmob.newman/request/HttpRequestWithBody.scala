package com.stackmob.newman.request

sealed trait HttpRequestWithBody extends HttpRequest {
  import HttpRequestWithBody._
  def body: RawBody
}

object HttpRequestWithBody {
  type RawBody = Array[Byte]
  val EmptyRawBody = Array[Byte]()
}

trait PostRequest extends HttpRequestWithBody
trait PutRequest extends HttpRequestWithBody