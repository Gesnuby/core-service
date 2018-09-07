package org.gesnuby.vetclinic.model

import cats.data.NonEmptyList

sealed trait UserError {
  def message: String
}

object UserError {
  type UserErrors = NonEmptyList[UserError]
  val userNotFound: UserError = UserNotFound
  val invalidCredentials: UserError = InvalidCredentials
  val alreadyLoggedIn: UserError = AlreadyLoggedIn
}

case class UserAlreadyExists(login: String) extends UserError {
  override def message: String = s"User '$login' already exists"
}

case object UserNotFound extends UserError {
  override def message: String = "User doesn't exist"
}

case class EmailIsInvalid(email: String) extends UserError {
  override def message: String = s"Email '$email' is invalid"
}

case object InvalidCredentials extends UserError {
  override def message: String = "Invalid login or password"
}

case object AlreadyLoggedIn extends UserError {
  override def message: String = "User already logged in"
}