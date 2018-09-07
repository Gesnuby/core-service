package org.gesnuby.vetclinic.endpoint

import cats.effect.{Effect, IO, Sync}
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.gesnuby.vetclinic.SecurityConfig
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.model.{User, UserSignupRequest, UserUpdateRequest}
import org.gesnuby.vetclinic.repository.InMemoryKVStore
import org.gesnuby.vetclinic.repository.interpreter.{InMemoryUserRepository, UserBackingStore}
import org.gesnuby.vetclinic.security.Auth
import org.gesnuby.vetclinic.security.Auth.Cookie
import org.gesnuby.vetclinic.service.{AuthService, UserService}
import org.gesnuby.vetclinic.validation.interpreter.UserValidationInterpreter
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService, Method, Request, Response, Uri}
import org.scalatest.{FreeSpec, Matchers}
import scalacache.caffeine.CaffeineCache
import tsec.authentication.SecuredRequestHandler

import scala.util.Random

abstract class UserEndpointTest[F[_]: Effect] extends FreeSpec with Matchers with Http4sDsl[F] {

  def runEffect(test: => F[Unit]): Unit

  private def runRequest(endpoint: HttpService[F], request: Request[F]): F[Response[F]] = {
    endpoint.run(request).getOrElse(fail("Request wasn't handled"))
  }

  private val cookieCache = CaffeineCache[Auth.Cookie]
  private val userRepo = InMemoryUserRepository[F](new InMemoryKVStore[F, UserId, User])
  private val authenticator = Auth.createAuthenticator(UserBackingStore[F](userRepo), cookieCache, SecurityConfig("auth"))
  private val validation = UserValidationInterpreter[F](userRepo)
  private val authService = AuthService[F](userRepo)
  private val userService = UserService[F](userRepo, validation, authService)
  private val endpoint = UserEndpoint.endpoints[F](SecuredRequestHandler(authenticator), userService)
  private val authenticatedUserF = User[F]("admin", "password", "admin@mail.com")

  private def randomLogin: F[String] = Sync[F].delay(Random.alphanumeric.take(5).toString())
  private def randomUserSignup: F[UserSignupRequest] = randomLogin.map(UserSignupRequest(_, "password", "test@email.com"))

  // Fixture for running test as authenticated user
  private def withAuth(testCode: Cookie => F[Any]): Unit = {
    val test = for {
      authUser <- authenticatedUserF
      // create user
      _ <- userRepo.create(authUser)
      // authenticate user
      auth <- authenticator.create(authUser.id)
      // pass authenticated cookie to test
      _ <- testCode(auth)
    } yield ()

    // run test
    runEffect(test)
  }

  // Fixture for providing already created user for test
  private def withExistingUser(testCode: User => F[Any])(implicit authCookie: Cookie): F[Any] = {
    for {
      userSignup <- randomUserSignup
      createRequest <- Request[F](Method.POST, Uri.uri("/"))
        .withBody(userSignup.asJson)
        .addCookie(authCookie.toCookie)
      createResponse <- runRequest(endpoint, createRequest)
      createdUser <- createResponse.as[User]
      test <- testCode(createdUser)
    } yield test
  }

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf[F, User]

  "User endpoints" - {
    "[GET /id] Get user" in withAuth { implicit cookie =>
      withExistingUser { user =>
        val getRequest = Request[F](Method.GET, Uri.unsafeFromString(s"/${user.id}")).addCookie(cookie.toCookie)
        for {
          getResponse <- runRequest(endpoint, getRequest)
          maybeUser <- getResponse.as[User]
        } yield {
          getResponse.status shouldBe Ok
          maybeUser.email shouldBe user.email
        }
      }
    }
    "[POST /] Create user" in withAuth { cookie =>
      for {
        userSignup <- randomUserSignup
        request <- Request[F](Method.POST, Uri.uri("/"))
          .withBody(userSignup.asJson)
          .addCookie(cookie.toCookie)
        response <- runRequest(endpoint, request)
      } yield {
        response.status shouldBe Created
      }
    }
    "[PUT /id] Update user" in withAuth { implicit cookie =>
      withExistingUser { user =>
        val update = UserUpdateRequest("admin@email.com")
        for {
          updateRequest <- Request[F](Method.PUT, Uri.unsafeFromString(s"/${user.id}"))
            .withBody(update.asJson)
            .addCookie(cookie.toCookie)
          updateResponse <- runRequest(endpoint, updateRequest)
          updatedUser <- updateResponse.as[User]
        } yield {
          update.email should not be user.email
          updateResponse.status shouldBe Ok
          updatedUser.email shouldBe update.email
        }
      }
    }
    "[DELETE /id] Delete user" in withAuth { implicit cookie =>
      withExistingUser { user =>
        val deleteRequest = Request[F](Method.DELETE, Uri.unsafeFromString(s"/${user.id}")).addCookie(cookie.toCookie)
        for {
          deleteResponse <- runRequest(endpoint, deleteRequest)
        } yield {
          deleteResponse.status shouldBe Ok
        }
      }
    }
  }
}

class IOUserEndpointTest extends UserEndpointTest[IO] {
  override def runEffect(test: => IO[Unit]): Unit = test.unsafeRunSync()
}