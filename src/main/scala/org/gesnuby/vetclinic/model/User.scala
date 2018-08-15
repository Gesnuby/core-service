package org.gesnuby.vetclinic.model

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import org.gesnuby.vetclinic.model.User.UserId

case class User(id: UserId, login: String, password: String, email: String)

object User {
  type UserId = UUID

  def uniqueId[F[_]: Sync]: F[UserId] =
    Sync[F].delay(UUID.randomUUID())

  def apply[F[_]: Sync](login: String, password: String, email: String): F[User] =
    uniqueId.map(new User(_, login, password, email))
}

case class LoginRequest(login: String, password: String)
case class UserSignupRequest(login: String, password: String, email: String)
case class UserUpdateRequest(email: String)