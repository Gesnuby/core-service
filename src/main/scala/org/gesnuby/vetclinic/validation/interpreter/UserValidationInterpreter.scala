package org.gesnuby.vetclinic.validation.interpreter

import cats.Applicative
import cats.data.EitherT
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import org.gesnuby.vetclinic.validation.algebra.UserValidation

class UserValidationInterpreter[F[_]: Applicative](userRepo: UserRepository[F]) extends UserValidation[F] {
  import cats.implicits._

  /**
    * Check if user login is unique across all other users
    */
  def loginIsUnique(user: User): EitherT[F, String, Unit] = EitherT {
    userRepo.loginExists(user.login) map {
      case true => Left("User already exists")
      case false => Right(())
    }
  }
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](userRepo: UserRepository[F]): UserValidationInterpreter[F] =
    new UserValidationInterpreter(userRepo)
}
