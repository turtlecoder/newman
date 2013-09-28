package com.stackmob.newman.test

import org.scalacheck.{Arbitrary, Gen}
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer._
import java.nio.ByteBuffer
import com.stackmob.newman.FinagleHttpClient.RichRawBody

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.test.finagle
 *
 * User: aaron
 * Date: 9/27/13
 * Time: 3:19 PM
 */
package object finagle {
  val genHttpResponseStatus: Gen[HttpResponseStatus] = {
    Gen.value(HttpResponseStatus.OK)
  }

  val genNonEmptyString: Gen[String] = Gen.alphaStr.suchThat { s =>
    s.length > 0
  }

  val genHeader: Gen[(String, String)] = {
    for {
      headerName <- genNonEmptyString
      headerVal <- genNonEmptyString
    } yield {
      headerName -> headerVal
    }
  }

  val genHeaders: Gen[Map[String, String]] = {
    Gen.listOf(genHeader).suchThat { l =>
      l.length > 0
    }.map { list =>
      list.toMap
    }
  }

  val genByte: Gen[Byte] = {
    Arbitrary.arbitrary[Byte]
  }

  val genByteArray: Gen[Array[Byte]] = {
    Gen.listOf(genByte).suchThat { list =>
      list.length > 0
    }.map { list =>
      list.toArray
    }
  }

  val genChannelBuffer: Gen[ChannelBuffer] = {
    genByteArray.map { arr =>
      arr.toChannelBuf
    }
  }

  val genNettyResponse: Gen[(HttpResponseStatus, Map[String, String], ChannelBuffer, HttpResponse)] = {
    for {
      status <- genHttpResponseStatus
      headers <- genHeaders
      body <- genChannelBuffer
    } yield {
      val resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
      headers.foreach { tup =>
        val (headerName, headerValue) = tup
        resp.setHeader(headerName, headerValue)
      }
      resp.setContent(body)
      (status, headers, body, resp)
    }
  }
}
