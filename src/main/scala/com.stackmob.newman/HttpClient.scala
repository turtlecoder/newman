package com.stackmob.newman

import com.stackmob.newman.request._
import java.net.URL
import com.stackmob.newman.request.HttpRequest._
import com.stackmob.newman.request.HttpRequestWithBody._

trait HttpClient {
  def get(url: URL, headers: Headers): GetRequest
  def post(url: URL, headers: Headers, body: RawBody): PostRequest
  def put(url: URL, headers: Headers, body: RawBody): PutRequest
  def delete(url: URL, headers: Headers): DeleteRequest
  def head(url: URL, headers: Headers): HeadRequest
}
