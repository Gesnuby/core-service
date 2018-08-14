package org.gesnuby.vetclinic.service

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.model.{User, UserSignupRequest, UserUpdateRequest}
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import org.gesnuby.vetclinic.validation.algebra.UserValidation

class UserService[F[_]: Sync](userRepo: UserRepository[F],
                              userValidation: UserValidation[F],
                              authService: AuthService[F]) {

  /**
    * Find all users
    */
  def getUsers: Stream[F, User] =
    userRepo.all

  /**
    * Find user by it's id
    */
  def getUser(id: UserId): EitherT[F, String, User] =
    EitherT.fromOptionF(userRepo.get(id), "User not found")

  /**
    * Create new user
    * User's login must be unique
    */
  def createUser(userSignup: UserSignupRequest): EitherT[F, String, User] = {
    for {
      user <- EitherT.right[String](userFromSignupRequest(userSignup))
      _ <- userValidation.loginIsUnique(user)
      createdUser <- EitherT.right[String](userRepo.create(user))
    } yield createdUser
  }

  /**
    * Update user info
    */
  def updateUser(userId: UserId, userUpdate: UserUpdateRequest): EitherT[F, String, User] = {
    for {
      user <- getUser(userId)
      userWithUpdates <- EitherT.right[String](userFromUpdateRequest(user, userUpdate))
      updatedUser <- EitherT.fromOptionF(userRepo.update(userWithUpdates), "User not found")
    } yield updatedUser
  }

  /**
    * Delete user by it's id
    */
  def deleteUser(id: UserId): EitherT[F, String, UserId] =
    EitherT.fromOptionF(userRepo.delete(id), "User not found")

  /**
    * UserSignupRequest -> User
    *
    * Create new User object from UserSignupRequest
    */
  private def userFromSignupRequest(req: UserSignupRequest): F[User] =
    for {
      hashedPassword <- authService.hashPassword(req.password)
    } yield User(req.login, hashedPassword, req.email)

  /**
    * (User, UserUpdateRequest) -> User
    *
    * Create new User object from existing User and UserUpdateRequest (by merging fields)
    */
  private def userFromUpdateRequest(user: User, req: UserUpdateRequest): F[User] =
    user.copy(email = req.email).pure[F]
}

object UserService {
  def apply[F[_]: Sync](userRepo: UserRepository[F],
                        userValidation: UserValidation[F],
                        authService: AuthService[F]): UserService[F] =
    new UserService(userRepo, userValidation, authService)
}
