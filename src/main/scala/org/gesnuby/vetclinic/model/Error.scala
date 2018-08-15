package org.gesnuby.vetclinic.model

import cats.data.NonEmptyList

sealed trait Error {
  def message: String
}

object Error {
  type Errors = NonEmptyList[Error]
  val userNotFound: Error = UserNotFound
}

case class UserAlreadyExists(login: String) extends Error {
  override def message: String = s"User '$login' already exists"
}

case object UserNotFound extends Error {
  override def message: String = "User doesn't exist"
}

case class EmailIsInvalid(email: String) extends Error {
  override def message: String = s"Email '$email' is invalid"
}
