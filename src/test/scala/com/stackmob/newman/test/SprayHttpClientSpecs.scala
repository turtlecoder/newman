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

package com.stackmob.newman.test

import org.specs2.Specification
import com.stackmob.newman.SprayHttpClient
import java.net.URL
import spray.can.Http
import spray.can.Http.HostConnectorSetup
import spray.can.client.{HostConnectorSettings, ClientConnectionSettings}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import spray.can.parsing.ParserSettings
import akka.actor.ActorSystem

class SprayHttpClientSpecs extends Specification with ClientTests with ResponseMatcher { def is =
  "SprayHttpClientSpecs".title                                                                                          ^ end ^
  "SprayHttpClient is a spray-based nonblocking HTTP client"                                                            ^ end ^
  "get should work"                                                                                                     ! ClientTests(client).get ^
  "getAsync should work"                                                                                                ! ClientTests(client).getAsync ^
  "post should work"                                                                                                    ! ClientTests(client).post ^ end ^
  "postAsync should work"                                                                                               ! ClientTests(client).postAsync ^ end ^
  "put should work"                                                                                                     ! ClientTests(client).put ^ end ^
  "putAsync should work"                                                                                                ! ClientTests(client).putAsync ^ end ^
  "delete should work"                                                                                                  ! ClientTests(client).delete ^ end ^
  "deleteAsync should work"                                                                                             ! ClientTests(client).deleteAsync ^ end ^
  "head should work"                                                                                                    ! ClientTests(client).head ^ end ^
  "headAsync should work"                                                                                               ! ClientTests(client).headAsync ^ end ^
  end

  /**
   * the SprayHttpClient used in testing. tuned to maximize test speed.
   * see https://github.com/spray/spray/blob/master/spray-can/src/main/resources/reference.conf
   * for an explanation of most of these config options.
   * @return the client
   */
  private def client = new SprayHttpClient(hostConnectorSetupFn = { url: URL =>
    val requestTimeout = Duration(500, TimeUnit.MILLISECONDS)
    val idleTimeout = Duration.Inf
    val reapingCycle = Duration(100, TimeUnit.MILLISECONDS)
    val connectingTimeout = Duration(500, TimeUnit.MILLISECONDS)
    val clientConnSettings = ClientConnectionSettings(
      userAgentHeader = "Stackmob/Newman (automated testing)",
      sslEncryption = false,
      idleTimeout = idleTimeout,
      requestTimeout = requestTimeout,
      reapingCycle = reapingCycle,
      responseChunkAggregationLimit = 500,
      requestSizeHint = 250,
      connectingTimeout = connectingTimeout,
      parserSettings = ParserSettings.apply(ActorSystem())
    )
    val hostConnectorSettings = HostConnectorSettings(
      maxConnections = 2,
      maxRetries = 1,
      pipelining = false,
      idleTimeout = idleTimeout,
      connectionSettings = clientConnSettings
    )
    HostConnectorSetup(url.getHost, port = 80, settings = Some(hostConnectorSettings))
  })
//  private def client = new SprayHttpClient()
//  private def client = new SprayHttpClient(mbCloseCmd = Some(Http.Close))

}
