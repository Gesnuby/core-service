package org.gesnuby.vetclinic.repository.interpreter

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import tsec.authentication.BackingStore

/**
  * Adapter between BackingStore and UserRepository
  */
class UserBackingStore[F[_]: Sync](userRepo: UserRepository[F]) extends BackingStore[F, UserId, User] {

  def put(user: User): F[User] =
    userRepo.create(user)

  def update(user: User): F[User] =
    userRepo.update(user).flatMap {
      case Some(u) => u.pure[F]
      case None => Sync[F].raiseError(new IllegalArgumentException)
    }

  def delete(id: UserId): F[Unit] =
    userRepo.delete(id).as(())

  def get(id: UserId): OptionT[F, User] =
    OptionT(userRepo.get(id))
}

object UserBackingStore {
  def apply[F[_]: Sync](userRepo: UserRepository[F]): UserBackingStore[F] = new UserBackingStore[F](userRepo)
}
