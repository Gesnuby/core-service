package org.gesnuby.vetclinic.service

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import org.gesnuby.vetclinic.model.UserError.{UserErrors, userNotFound}
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.model.{UserError, User, UserNotFound, UserSignupRequest, UserUpdateRequest}
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
  def getUser(id: UserId): EitherT[F, UserError, User] =
    EitherT.fromOptionF(userRepo.get(id), userNotFound)

  /**
    * Create new user
    * User's login must be unique
    */
  def createUser(userSignup: UserSignupRequest): EitherT[F, UserErrors, User] = {
    for {
      user <- EitherT.right[UserErrors](userFromSignupRequest(userSignup))
      validUser <- EitherT(userValidation.validateNewUser(user).map(_.toEither))
      createdUser <- EitherT.right[UserErrors](userRepo.create(validUser))
    } yield createdUser
  }

  /**
    * Update user info
    */
  def updateUser(userId: UserId, userUpdate: UserUpdateRequest): EitherT[F, UserError, User] = {
    for {
      user <- getUser(userId)
      userWithUpdates <- EitherT.right[UserError](userFromUpdateRequest(user, userUpdate))
      validUser <- EitherT(userValidation.emailIsValid(userWithUpdates).map(_.toEither))
      updatedUser <- EitherT.fromOptionF(userRepo.update(validUser), userNotFound)
    } yield updatedUser
  }

  /**
    * Delete user by it's id
    */
  def deleteUser(id: UserId): EitherT[F, UserError, UserId] =
    EitherT.fromOptionF(userRepo.delete(id), userNotFound)

  /**
    * UserSignupRequest -> User
    *
    * Create new User object from UserSignupRequest
    */
  private def userFromSignupRequest(req: UserSignupRequest): F[User] =
    for {
      hashedPassword <- authService.hashPassword(req.password)
      user <- User(req.login, hashedPassword, req.email)
    } yield user

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
