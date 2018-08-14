package org.gesnuby.vetclinic.endpoint

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.gesnuby.vetclinic.http.UUIDVar
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.model.{User, UserSignupRequest, UserUpdateRequest}
import org.gesnuby.vetclinic.security.Auth.{Cookie, SecuredHandler, SecuredService}
import org.gesnuby.vetclinic.service.UserService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, HttpService, MediaType}
import tsec.authentication.{SecuredRequestHandler, TSecAuthService, _}

class UserEndpoint[F[_]: Sync](securedRequestHandler: SecuredHandler[F],
                               userService: UserService[F]) extends Http4sDsl[F] {

  implicit val signupDecoder: EntityDecoder[F, UserSignupRequest] = jsonOf[F, UserSignupRequest]
  implicit val updateDecoder: EntityDecoder[F, UserUpdateRequest] = jsonOf[F, UserUpdateRequest]

  implicit class StreamToJson[M[_], V: Encoder](values: fs2.Stream[M, V]) {
    import fs2.Stream
    def asJsonArray: Stream[M, String] = {
      Stream.emit("[") ++ values.map(_.asJson.noSpaces).intersperse(",") ++ Stream.emit("]")
    }
  }

  private def getUsersEndpoint: HttpService[F] = HttpService[F] {
    case GET -> Root =>
      Ok(userService.getUsers.asJsonArray, `Content-Type`(MediaType.`application/json`))
  }

  private def getUserEndpoint: SecuredService[F] = TSecAuthService {
    case GET -> Root / UUIDVar(id) asAuthed _ =>
      userService.getUser(id).value.flatMap {
        case Right(user) => Ok(user.asJson)
        case Left(error) => NotFound(error)
      }
  }

  private def createUserEndpoint: SecuredService[F] = TSecAuthService {
    case req @ POST -> Root asAuthed _ =>
      val action = for {
        signup <- req.request.as[UserSignupRequest]
        result <- userService.createUser(signup).value
      } yield result

      action.flatMap {
        case Right(user) => Ok(user.asJson)
        case Left(error) => Conflict(error)
      }
  }

  private def updateUserEndpoint: SecuredService[F] = TSecAuthService {
    case req @ PUT -> Root / UUIDVar(id) asAuthed _ =>
      val action = for {
        update <- req.request.as[UserUpdateRequest]
        updatedUser <- userService.updateUser(id, update).value
      } yield updatedUser

      action.flatMap {
        case Right(user) => Ok(user.asJson)
        case Left(error) => NotFound(error)
      }
  }

  private def deleteUserEndpoint: SecuredService[F] = TSecAuthService {
    case DELETE -> Root / UUIDVar(id) asAuthed _ =>
      userService.deleteUser(id).value.flatMap {
        case Right(_) => Ok("deleted")
        case Left(error) => NotFound(error)
      }
  }

  def endpoints: HttpService[F] = getUsersEndpoint <+> securedRequestHandler.liftWithFallthrough(
    getUserEndpoint <+>
      createUserEndpoint <+> updateUserEndpoint <+>
      deleteUserEndpoint)
}

object UserEndpoint {
  def endpoints[F[_]: Sync](securedRequestHandler: SecuredHandler[F],
                            userService: UserService[F]): HttpService[F] =
    new UserEndpoint[F](securedRequestHandler, userService).endpoints
}