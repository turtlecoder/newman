/**
 * Copyright 2012-2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.newman

import com.stackmob.newman.request._
import java.net.URL
import com.stackmob.newman.request.HttpRequest._
import com.stackmob.newman.request.HttpRequestWithBody._

trait HttpClient {
  /**
   * perform an HTTP GET request
   * @param url the URL against which to perform the request
   * @param headers the headers to accompany the request
   * @return the {{{GetRequest}}} that can execute the request with the given parameters
   */
  def get(url: URL, headers: Headers): GetRequest

  /**
   * perform an HTTP POST request
   * @param url the URL against which to perform the request
   * @param headers the headers to accompany the request
   * @param body the body to accompany the request
   * @return the {{{PostRequest}}} that can execute the request with the given parameters
   */
  def post(url: URL, headers: Headers, body: RawBody): PostRequest

  /**
   * perform an HTTP PUT request
   * @param url the URL against which to perform the request
   * @param headers the headers to accompany the request
   * @param body the body to accompany the request
   * @return the {{{PutRequest}}} that can execute the request with the given parameters
   */
  def put(url: URL, headers: Headers, body: RawBody): PutRequest

  /**
   * perform an HTTP DELETE request
   * @param url the URL against which to perform the request
   * @param headers the headers to accompany the request
   * @return the {{{DeleteRequest}}} that can execute the request with the given parameters
   */
  def delete(url: URL, headers: Headers): DeleteRequest

  /**
   * perform an HTTP HEAD request
   * @param url the URL against which to perform the request
   * @param headers the headers to accompany the request
   * @return the {{{HeadRequest}}} that can execute the request with the given parameters
   */
  def head(url: URL, headers: Headers): HeadRequest
}
