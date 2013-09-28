package com.stackmob.newman.test.client

import com.stackmob.newman.Headers

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.test.client
 *
 * User: aaron
 * Date: 9/27/13
 * Time: 4:11 PM
 */

case class ExpectedResponse(url: String,
                            headers: Headers,
                            args: Map[String, String],
                            origin: String,
                            mbData: Option[String])