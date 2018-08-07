package org.gesnuby.vetclinic.endpoint

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.security.Auth
import org.gesnuby.vetclinic.security.Auth.{Cookie, LoginRequest, SecuredService}
import org.gesnuby.vetclinic.service.AuthService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService, Request, Response}
import tsec.authentication.{Authenticator, TSecAuthService, _}

class AuthEndpoint[F[_]: Sync](authenticator: Authenticator[F, UserId, User, Cookie], authService: AuthService[F]) extends Http4sDsl[F] {

  type Req = Request[F]

  implicit val loginDecoder: EntityDecoder[F, LoginRequest] = jsonOf[F, LoginRequest]

  private def loginEndpoint: HttpService[F] = HttpService[F] {
    case req @ POST -> Root / "login" =>
      val action = for {
        request <- checkAlreadyLoggedIn(req)
        login <- EitherT.right[String](request.as[LoginRequest])
        user <- authService.verifyLogin(login)
        cookie <- EitherT.right[String](authenticator.create(user.id))
      } yield cookie
      action.value.flatMap {
        case Right(cookie) => Response[F]().addCookie(cookie.toCookie).pure[F]
        case Left(error) => BadRequest(error)
      }
  }

  /**
    * Check if user is already logged in
    */
  private def checkAlreadyLoggedIn(req: Req): EitherT[F, String, Req] =
    EitherT.fromOptionF(authenticator.extractAndValidate(req).value, req)
      .swap
      .leftMap(_ => "Already logged in")

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
  def endpoints[F[_]: Sync](authenticator: Authenticator[F, UserId, User, Cookie], authService: AuthService[F]): HttpService[F] =
    new AuthEndpoint[F](authenticator, authService).endpoints
}
