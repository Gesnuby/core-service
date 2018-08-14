package org.gesnuby.vetclinic.repository

import cats.effect.Sync

import scala.collection.concurrent.TrieMap

/**
  * Key-Value store
  *
  * @tparam F effect type (Task, IO, Future, etc)
  * @tparam S stream type
  * @tparam K key type
  * @tparam V value type
  */
trait KVStore[F[_], S[_[_], _], K, V] {

  /**
    * Retrieve record from the store
    *
    * @param key record key
    * @return Some(record) or None if record doesn't exist
    */
  def get(key: K): F[Option[V]]

  /**
    * Add new record to the store
    *
    * @param key record key
    * @param value record
    * @return addedRecord
    */
  def put(key: K, value: V): F[V]

  /**
    * Update existing record
    *
    * @param key record key
    * @param value record
    * @return Some(updatedRecord) or None if record doesn't exist
    */
  def update(key: K, value: V): F[Option[V]]

  /**
    * Remove record from the store
    *
    * @param key record key
    * @return Some(removedRecord) or None if record doesn't exist
    */
  def delete(key: K): F[Option[V]]

  /**
    * Return all records from the store
    *
    * @return stream of records
    */
  def values: S[F, V]

  /**
    * Remove all records
    */
  def clear: F[Unit]
}

import fs2.Stream

class InMemoryKVStore[F[_]: Sync, K, V] extends KVStore[F, Stream, K, V] {
  import cats.implicits._

  private val F = Sync[F]

  private val store = new TrieMap[K, V]()

  def get(key: K): F[Option[V]] =
    store.get(key).pure[F]

  def put(key: K, value: V): F[V] = F.pure {
    store.put(key, value).fold(value)(_ => value)
  }

  def update(key: K, value: V): F[Option[V]] = F.delay {
    store.replace(key, value).as(value)
  }

  def delete(key: K): F[Option[V]] = F.delay {
    store.remove(key)
  }

  def values: Stream[F, V] = {
    val iterator = store.valuesIterator
    Stream.unfold(iterator)(i => if (i.hasNext) Some(i.next(), i) else None)
  }

  def clear: F[Unit] = F.delay {
    store.clear()
  }
}