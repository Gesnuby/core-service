package org.gesnuby.vetclinic.repository.interpreter

import cats.effect.Sync
import fs2.Stream
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.repository.InMemoryKVStore
import org.gesnuby.vetclinic.repository.algebra.UserRepository

class InMemoryUserRepository[F[_]: Sync] extends UserRepository[F] {
  import cats.implicits._

  val cache = new InMemoryKVStore[F, UserId, User]

  def all: Stream[F, User] =
    cache.values

  def get(userId: UserId): F[Option[User]] =
    cache.get(userId)

  def create(user: User): F[Option[User]] =
    cache.put(user.id, user).map(Some(_))

  def update(user: User): F[Option[User]] =
    cache.update(user.id, user)

  def delete(userId: UserId): F[Option[User]] =
    cache.delete(userId)

  def loginExists(login: String): F[Boolean] =
    cache.values.exists(_.login == login).compile.toList.map(_.head)

  def findByLogin(login: String): F[Option[User]] =
    cache.values.find(_.login == login).compile.toList.map(_.headOption)
}

object InMemoryUserRepository {
  def apply[F[_]: Sync](): InMemoryUserRepository[F] = new InMemoryUserRepository[F]
}