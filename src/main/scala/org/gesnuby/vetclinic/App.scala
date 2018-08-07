package org.gesnuby.vetclinic

import cats.effect.{Effect, IO}
import fs2.{Stream, StreamApp}
import org.gesnuby.vetclinic.endpoint.{AuthEndpoint, StaticFilesEndpoint, UserEndpoint}
import org.gesnuby.vetclinic.model.UserSignupRequest
import org.gesnuby.vetclinic.repository.interpreter.{InMemoryUserRepository, UserBackingStore}
import org.gesnuby.vetclinic.security.Auth
import org.gesnuby.vetclinic.service.{AuthService, UserService}
import org.gesnuby.vetclinic.validation.interpreter.UserValidationInterpreter
import org.http4s.server.blaze.BlazeBuilder
import tsec.authentication.SecuredRequestHandler

import scala.concurrent.ExecutionContext.Implicits.global

object App extends HttpApp[IO]

class HttpApp[F[_] : Effect] extends StreamApp[F] {
  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, StreamApp.ExitCode] = {
    val adminSignup = UserSignupRequest("admin", "password", "admin@mail.ru")
    for {
      config <- Stream.eval(Configuration.load[F])
      userRepo = InMemoryUserRepository[F]()
      authenticator = Auth.createAuthenticator(UserBackingStore[F](userRepo))
      securedRequestHandler = SecuredRequestHandler(authenticator)
      userValidation = UserValidationInterpreter[F](userRepo)
      authService = AuthService[F](userRepo)
      userService = UserService[F](userRepo, userValidation, authService)
      _ <- Stream.eval(userService.createUser(adminSignup).value)
      exitCode <- BlazeBuilder[F]
        .bindHttp(port = config.port, host = config.host)
        .mountService(StaticFilesEndpoint.endpoints[F](), "/static")
        .mountService(UserEndpoint.endpoints[F](securedRequestHandler, userService), "/api/users")
        .mountService(AuthEndpoint.endpoints[F](authenticator, authService), prefix = "/auth")
        .serve
    } yield exitCode
  }
}
