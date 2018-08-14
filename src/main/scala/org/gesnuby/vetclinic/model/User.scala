package org.gesnuby.vetclinic.model

import java.util.UUID

import org.gesnuby.vetclinic.model.User.UserId

case class User(id: UserId, login: String, password: String, email: String)

object User {
  type UserId = UUID
  def uniqueId: UserId = UUID.randomUUID()
  def apply(login: String, password: String, email: String): User = new User(uniqueId, login, password, email)
}

case class LoginRequest(login: String, password: String)
case class UserSignupRequest(login: String, password: String, email: String)
case class UserUpdateRequest(email: String)