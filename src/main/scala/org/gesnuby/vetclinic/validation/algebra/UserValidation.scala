package org.gesnuby.vetclinic.validation.algebra

import cats.data.EitherT
import org.gesnuby.vetclinic.model.User

trait UserValidation[F[_]] {
  def loginIsUnique(user: User): EitherT[F, String, Unit]
}
