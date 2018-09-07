package org.gesnuby.vetclinic.endpoint

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.gesnuby.vetclinic.model.UserError.alreadyLoggedIn
import org.gesnuby.vetclinic.model.{UserError, LoginRequest}
import org.gesnuby.vetclinic.security.Auth.{AppAuthenticator, SecuredService}
import org.gesnuby.vetclinic.service.AuthService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService, Request, Response}
import tsec.authentication.{TSecAuthService, _}

class AuthEndpoint[F[_]: Sync](authenticator: AppAuthenticator[F], authService: AuthService[F]) extends Http4sDsl[F] {

  type Req = Request[F]

  implicit val loginDecoder: EntityDecoder[F, LoginRequest] = jsonOf[F, LoginRequest]
  implicit val errorEncoder: Encoder[UserError] = (error: UserError) => Json.fromString(error.message)

  private def loginEndpoint: HttpService[F] = HttpService[F] {
    case req @ POST -> Root / "login" =>
      val action = for {
        request <- checkAlreadyLoggedIn(req)
        login <- EitherT.right[UserError](request.as[LoginRequest])
        user <- authService.verifyLogin(login)
        cookie <- EitherT.right[UserError](authenticator.create(user.id))
      } yield cookie
      action.value.flatMap {
        case Right(cookie) => Response[F]().addCookie(cookie.toCookie).pure[F]
        case Left(error) => BadRequest(error.asJson)
      }
  }

  /**
    * Check if user is already logged in
    */
  private def checkAlreadyLoggedIn(req: Req): EitherT[F, UserError, Req] =
    EitherT.fromOptionF(authenticator.extractAndValidate(req).value, req)
      .swap
      .leftMap(_ => alreadyLoggedIn)

  private def logoutEndpoint: SecuredService[F] = TSecAuthService {
    case req @ POST -> Root / "logout" asAuthed _ =>
      val action = for {
        _ <- authenticator.discard(req.authenticator)
      } yield ()
      action.flatMap { _ =>
        Response[F]().addCookie(req.authenticator.toCookie.copy(content = "")).pure[F]
      }
  }

  def endpoints: HttpService[F] = loginEndpoint <+> SecuredRequestHandler(authenticator).liftWithFallthrough(logoutEndpoint)
}

object AuthEndpoint {
  def endpoints[F[_]: Sync](authenticator: AppAuthenticator[F], authService: AuthService[F]): HttpService[F] =
    new AuthEndpoint[F](authenticator, authService).endpoints
}
