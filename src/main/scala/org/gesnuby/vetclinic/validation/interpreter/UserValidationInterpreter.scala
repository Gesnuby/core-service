package org.gesnuby.vetclinic.validation.interpreter

import cats.Applicative
import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import org.apache.commons.validator.routines.EmailValidator
import org.gesnuby.vetclinic.model.{EmailIsInvalid, Error, User, UserAlreadyExists}
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import org.gesnuby.vetclinic.validation.algebra.UserValidation

class UserValidationInterpreter[F[_]: Applicative](userRepo: UserRepository[F]) extends UserValidation[F] {

  def loginIsUnique(user: User): F[Validated[Error, User]] = {
    userRepo.findByLogin(user.login).map { maybeUser =>
      Validated.cond(maybeUser.isEmpty, user, UserAlreadyExists(user.login))
    }
  }

  def emailIsValid(user: User): F[Validated[Error, User]] = {
    Applicative[F].pure(Validated.cond(
      EmailValidator.getInstance().isValid(user.email),
      user,
      EmailIsInvalid(user.email)))
  }

  def validateNewUser(user: User): F[ValidatedNel[Error, User]] = {
    (loginIsUnique(user), emailIsValid(user)).mapN {
      case (c1, c2) => (c1.toValidatedNel, c2.toValidatedNel).mapN((_, _) => user)
    }
  }
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](userRepo: UserRepository[F]): UserValidationInterpreter[F] =
    new UserValidationInterpreter(userRepo)
}
