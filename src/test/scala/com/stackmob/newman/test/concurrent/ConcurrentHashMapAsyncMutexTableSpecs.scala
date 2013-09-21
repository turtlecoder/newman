package com.stackmob.newman.test
package concurrent

import org.specs2.Specification
import com.stackmob.newman.concurrent.{InMemoryAsyncMutex, ConcurrentHashMapAsyncMutexTable}
import scala.concurrent._
import java.util.concurrent.Executors

class ConcurrentHashMapAsyncMutexTableSpecs extends Specification { def is =
  "ConcurrentHashMapAsyncMutexTableSpecs".title                                                                         ^ end ^
  "ConcurrentHashMapAsyncMutexTable is an AsyncMutexTable backed by a concurrent hash map for storage"                  ^ end ^
  "mutex should return the same mutex for the same key"                                                                 ! sameMutex ^ end ^
  end

  private def sameMutex = {
    val mutex = new InMemoryAsyncMutex
    val createMutex = () => mutex
    val table = ConcurrentHashMapAsyncMutexTable[String](createMutex)
    implicit lazy val ctx = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
    val first = Future(table.mutex("one")).block() must beEqualTo(mutex)
    val second = Future(table.mutex("two")).block() must beEqualTo(mutex)
    first and second
  }
}
