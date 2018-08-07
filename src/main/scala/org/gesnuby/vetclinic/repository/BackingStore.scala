package org.gesnuby.vetclinic.repository

import cats.data.OptionT
import cats.effect.{Async, Sync}
import cats.implicits._
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
        store.delete(id).map(_ => ())

      def get(id: I): OptionT[F, V] =
        OptionT(store.get(id))
    }

  /**
    * BackingStore backed by Redis
    */
  def redis[F[_], I, V: Codec](getId: V => I)(implicit F: Async[F]): BackingStore[F, I ,V] = {
    import scalacache._
    import scalacache.redis._
    import scalacache.CatsEffect.modes.async
    import scalacache.serialization.binary._

    implicit val redisCache: Cache[V] = RedisCache[V]("0.0.0.0", 32771)

    new BackingStore[F, I, V] {
      def put(v: V): F[V] =
        redisCache.put(getId(v))(v).map(_ => v)

      def update(v: V): F[V] =
        put(v)

      def delete(id: I): F[Unit] =
        redisCache.remove(id).map(_ => ())

      def get(id: I): OptionT[F, V] =
        OptionT(redisCache.get(id))
    }
  }
}
