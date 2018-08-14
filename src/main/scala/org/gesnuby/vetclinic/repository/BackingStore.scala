package org.gesnuby.vetclinic.repository

import cats.data.OptionT
import cats.effect.{Async, Sync}
import cats.implicits._
import scalacache.Cache
import scalacache.serialization.Codec
import tsec.authentication.BackingStore

object BackingStore {

  /**
    * In-memory BackingStore backed by InMemoryKVStore
    */
  def inMemory[F[_], I, V](getId: V => I)(implicit F: Sync[F]): BackingStore[F, I, V] =
    new BackingStore[F, I, V] {
      private val store = new InMemoryKVStore[F, I, V]

      def put(v: V): F[V] =
        store.put(getId(v), v)

      def update(v: V): F[V] =
        store.update(getId(v), v).flatMap {
          case Some(value) => value.pure[F]
          case None => F.raiseError(new IllegalArgumentException)
        }

      def delete(id: I): F[Unit] =
        store.delete(id).as(())

      def get(id: I): OptionT[F, V] =
        OptionT(store.get(id))
    }

  /**
    * BackingStore backed by scalacache Cache
    */
  def cached[F[_]: Async, I, V: Codec](getId: V => I)(implicit cache: Cache[V]): BackingStore[F, I ,V] = {
    import scalacache.CatsEffect.modes.async

    new BackingStore[F, I, V] {
      def put(v: V): F[V] =
        cache.put(getId(v))(v).as(v)

      def update(v: V): F[V] =
        put(v)

      def delete(id: I): F[Unit] =
        cache.remove(id).as(())

      def get(id: I): OptionT[F, V] =
        OptionT(cache.get(id))
    }
  }
}
