package org.gesnuby.vetclinic.repository.interpreter

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import tsec.authentication.BackingStore

/**
  * Adapter between [[tsec.authentication.BackingStore]] and [[org.gesnuby.vetclinic.repository.algebra.UserRepository]]
  */
class UserBackingStore[F[_]: Sync](userRepo: UserRepository[F]) extends BackingStore[F, UserId, User] {

  private val F = implicitly[Sync[F]]

  def put(user: User): F[User] =
    userRepo.create(user).flatMap {
      case Some(u) => u.pure[F]
      case None => F.raiseError(new IllegalArgumentException)
    }

  def update(user: User): F[User] =
    userRepo.update(user).flatMap {
      case Some(u) => u.pure[F]
      case None => F.raiseError(new IllegalArgumentException)
    }

  def delete(id: UserId): F[Unit] =
    userRepo.delete(id).map(_ => ())

  def get(id: UserId): OptionT[F, User] =
    OptionT(userRepo.get(id))
}

object UserBackingStore {
  def apply[F[_]: Sync](userRepo: UserRepository[F]): UserBackingStore[F] = new UserBackingStore[F](userRepo)
}
