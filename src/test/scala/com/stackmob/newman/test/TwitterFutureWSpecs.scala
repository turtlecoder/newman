package com.stackmob.newman.test

import org.specs2.Specification
import com.twitter.util._
import com.stackmob.newman.FinagleHttpClient._

class TwitterFutureWSpecs extends Specification { def is =
  "TwitterFutureSpecs".title                                                                                            ^ end ^
  "TwitterFutureW is a class extension for com.twitter.util.Future"                                                     ^ end ^
  "toScalaPromise should work properly"                                                                                 ! toScalaPromise ^ end ^
  end

  private def toScalaPromise = {
    val futureReturn = 1
    val fut = Future {
      futureReturn
    }

    val prom = fut.toScalaPromise
    prom.get must beEqualTo(futureReturn)
  }
}
