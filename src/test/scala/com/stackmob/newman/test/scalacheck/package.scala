package com.stackmob.newman.test

import com.stackmob.newman.caching.Time
import org.scalacheck.Gen
import java.util.concurrent.TimeUnit

/**
 * Created by IntelliJ IDEA.
 *
 * com.stackmob.newman.tests
 *
 * User: aaron
 * Date: 2/21/13
 * Time: 3:49 PM
 */
package object scalacheck {
  val genNonEmptyString: Gen[String] = for {
    firstChar <- Gen.listOf1(Gen.alphaChar)
    remainingChars <- Gen.listOf(Gen.alphaChar)
  } yield {
    val charList = List(firstChar) ++ remainingChars
    charList.mkString
  }

  private val timeUnits = Seq(TimeUnit.DAYS,
    TimeUnit.HOURS,
    TimeUnit.MICROSECONDS,
    TimeUnit.MILLISECONDS,
    TimeUnit.MINUTES,
    TimeUnit.NANOSECONDS,
    TimeUnit.SECONDS)

  val genTimeUnit: Gen[TimeUnit] = Gen.oneOf(timeUnits)

  val genPositiveTime: Gen[Time] = for {
    magnitude <- Gen.posNum[Int]
    timeUnit <- genTimeUnit
  } yield {
    Time(magnitude, timeUnit)
  }

}
