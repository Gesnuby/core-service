package org.gesnuby.vetclinic.repository.interpreter

import cats.effect.Sync
import fs2.Stream
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.repository.InMemoryKVStore
import org.gesnuby.vetclinic.repository.algebra.UserRepository

class InMemoryUserRepository[F[_]: Sync](private val store: InMemoryKVStore[F, UserId, User]) extends UserRepository[F] {
  import cats.implicits._

  def all: Stream[F, User] =
    store.values

  def get(userId: UserId): F[Option[User]] =
    store.get(userId)

  def create(user: User): F[User] =
    store.put(user.id, user)

  def update(user: User): F[Option[User]] =
    store.update(user.id, user)

  def delete(userId: UserId): F[Option[UserId]] =
    for {
      mu <- store.delete(userId)
    } yield mu.map(_.id)

  def findByLogin(login: String): F[Option[User]] =
    store.values.find(_.login == login).compile.toList.map(_.headOption)
}

object InMemoryUserRepository {
  def apply[F[_]: Sync](store: InMemoryKVStore[F, UserId, User]): InMemoryUserRepository[F] =
    new InMemoryUserRepository[F](store)
}