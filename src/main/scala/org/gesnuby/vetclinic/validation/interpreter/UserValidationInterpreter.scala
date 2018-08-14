package org.gesnuby.vetclinic.validation.interpreter

import cats.Applicative
import cats.data.EitherT
import cats.implicits._
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import org.gesnuby.vetclinic.validation.algebra.UserValidation

class UserValidationInterpreter[F[_]: Applicative](userRepo: UserRepository[F]) extends UserValidation[F] {

  /**
    * Check if user login is unique across all other users
    */
  def loginIsUnique(user: User): EitherT[F, String, Unit] = EitherT {
    userRepo.findByLogin(user.login).map { u =>
      Either.cond(u.isEmpty, (), "User already exists")
    }
  }
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](userRepo: UserRepository[F]): UserValidationInterpreter[F] =
    new UserValidationInterpreter(userRepo)
}
