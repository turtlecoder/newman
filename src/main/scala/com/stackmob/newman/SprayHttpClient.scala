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

import scala.concurrent.Future
import akka.actor._
import spray.client.HttpConduit
import spray.io._
import spray.util._
import spray.http._
import HttpMethods._
import spray.can.client.{HttpClient => NativeSprayHttpClient}
import java.net.URL
import com.stackmob.newman.request._

class SprayHttpClient(sprayHttpClient: SprayHttpClient = SprayHttpClient.DefaultSprayHttpClient) extends HttpClient {
  def get(url: URL, headers: Headers): GetRequest = {
    sys.error("not yet implemented")
  }
  def post(url: URL, headers: Headers, body: RawBody): PostRequest = {
    sys.error("not yet implemented")
  }
  def put(url: URL, headers: Headers, body: RawBody): PutRequest = {
    sys.error("not yet implemented")
  }
  def delete(url: URL, headers: Headers): DeleteRequest = {
    sys.error("not yet implemented")
  }
  def head(url: URL, headers: Headers): HeadRequest = {
    sys.error("not yet implemented")
  }
}

object SprayHttpClient {
  lazy val DefaultActorSystem = ActorSystem()
  lazy val DefaultIOBridge = IOExtension(DefaultActorSystem).ioBridge()
  class DefaultSprayHttpClient extends NativeSprayHttpClient(DefaultIOBridge)
  lazy val DefaultSprayHttpClient = DefaultActorSystem.actorOf(Props[DefaultSprayHttpClient])
}
