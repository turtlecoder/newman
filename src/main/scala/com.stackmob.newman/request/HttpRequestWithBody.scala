package com.stackmob.newman.request

import java.nio.charset.Charset
import com.stackmob.newman.Constants._

sealed trait HttpRequestWithBody extends HttpRequest {
  import HttpRequestWithBody._
  def body: RawBody
}

object HttpRequestWithBody {
  type RawBody = Array[Byte]

  object RawBody {
    private lazy val emptyBytes = Array[Byte]()
    def empty = emptyBytes
    def apply(s: String, charset: Charset = UTF8Charset) = s.getBytes(charset)
    def apply(b: Array[Byte]) = b
  }

}

trait PostRequest extends HttpRequestWithBody
trait PutRequest extends HttpRequestWithBody