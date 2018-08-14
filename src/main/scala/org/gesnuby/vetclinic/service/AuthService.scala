package org.gesnuby.vetclinic.service

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import de.mkammerer.argon2.Argon2Factory
import org.gesnuby.vetclinic.model.{LoginRequest, User}
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import org.gesnuby.vetclinic.security.ArgonSettings

class AuthService[F[_]: Sync](userRepo: UserRepository[F], argonSettings: ArgonSettings = ArgonSettings.default) {

  private val argon2 = Argon2Factory.create(argonSettings.argon2Type)

  /**
    * Check if supplied user login info (login and password) is correct
    */
  def verifyLogin(login: LoginRequest): EitherT[F, String, User] = {
    val maybeUser = EitherT.fromOptionF(userRepo.findByLogin(login.login), "Invalid login or password")
    maybeUser.flatMap { user =>
      checkPassword(user.password, login.password).map(_ => user)
    }
  }

  /**
    * Create hash from password
    */
  def hashPassword(password: String): F[String] = Sync[F].delay {
    argon2.hash(argonSettings.iterations, argonSettings.memory, argonSettings.parallelism, password)
  }

  /**
    * Check if password matches the hash
    */
  private def checkPassword(hashedPassword: String, password: String): EitherT[F, String, Unit] = {
    if (argon2.verify(hashedPassword, password)) {
      EitherT.right[String](().pure[F])
    } else {
      EitherT.left[Unit]("Invalid login or password".pure[F])
    }
  }
}

object AuthService {
  def apply[F[_]: Sync](userRepo: UserRepository[F],
                        argonSettings: ArgonSettings = ArgonSettings.default): AuthService[F] =
    new AuthService[F](userRepo, argonSettings)
}
