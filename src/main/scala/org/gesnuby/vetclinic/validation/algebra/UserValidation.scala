package org.gesnuby.vetclinic.validation.algebra

import cats.data.{Validated, ValidatedNel}
import org.gesnuby.vetclinic.model.{Error, User}

trait UserValidation[F[_]] {

  /**
    * Check if user login is unique across all other users
    */
  def loginIsUnique(user: User): F[Validated[Error, User]]

  /**
    * Check if email is valid
    */
  def emailIsValid(user: User): F[Validated[Error, User]]

  /**
    * Check if newly created user is valid
    */
  def validateNewUser(user: User): F[ValidatedNel[Error, User]]
}
