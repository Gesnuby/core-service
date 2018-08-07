package org.gesnuby.vetclinic.repository.algebra

import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import fs2.Stream

trait UserRepository[F[_]] {
  def all: Stream[F, User]
  def get(userId: UserId): F[Option[User]]
  def create(user: User): F[Option[User]]
  def update(user: User): F[Option[User]]
  def delete(userId: UserId): F[Option[User]]
  def loginExists(login: String): F[Boolean]
  def findByLogin(login: String): F[Option[User]]
}
