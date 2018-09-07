package org.gesnuby.vetclinic.service

import cats.data.EitherT
import cats.effect.Sync
import de.mkammerer.argon2.Argon2Factory
import org.gesnuby.vetclinic.model.UserError.invalidCredentials
import org.gesnuby.vetclinic.model.{UserError, LoginRequest, User}
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import org.gesnuby.vetclinic.security.ArgonSettings

class AuthService[F[_]: Sync](userRepo: UserRepository[F], argonSettings: ArgonSettings = ArgonSettings.default) {

  private val argon2 = Argon2Factory.create(argonSettings.argon2Type)

  /**
    * Check if supplied user login info (login and password) is correct
    */
  def verifyLogin(login: LoginRequest): EitherT[F, UserError, User] = {
    for {
      user <- EitherT.fromOptionF(userRepo.findByLogin(login.login), invalidCredentials)
      _ <- checkPassword(user.password, login.password)
    } yield user
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
  private def checkPassword(hashedPassword: String, password: String): EitherT[F, UserError, Unit] =
    EitherT.cond[F](argon2.verify(hashedPassword, password), (), invalidCredentials)
}

object AuthService {
  def apply[F[_]: Sync](userRepo: UserRepository[F],
                        argonSettings: ArgonSettings = ArgonSettings.default): AuthService[F] =
    new AuthService[F](userRepo, argonSettings)
}
