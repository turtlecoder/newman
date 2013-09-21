package com.stackmob.newman.concurrent

import java.util.concurrent.ConcurrentHashMap

/**
 * an AsyncMutexTable backed by a ConcurrentHashMap
 * @tparam Key the keys which identify each AsyncMutex in the table
 * @param f a function to create new {{{AsyncMutex}}}es to be used to fill the table if there's a miss
 */
class ConcurrentHashMapAsyncMutexTable[Key](f: () => AsyncMutex) extends AsyncMutexTable[Key] {
  private lazy val table = new ConcurrentHashMap[Key, AsyncMutex]()

  override def filler: AsyncMutex = f()

  override def mutex(key: Key): AsyncMutex = {
    val newMutex = filler
    Option(table.putIfAbsent(key, newMutex)).getOrElse(newMutex)
  }
}

object ConcurrentHashMapAsyncMutexTable {
  def apply[Key](filler: () => AsyncMutex): ConcurrentHashMapAsyncMutexTable[Key] = {
    new ConcurrentHashMapAsyncMutexTable[Key](filler)
  }
}