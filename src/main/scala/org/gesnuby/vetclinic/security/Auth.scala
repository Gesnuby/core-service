package org.gesnuby.vetclinic.security

import java.util.UUID

import cats.Id
import cats.effect.Async
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.repository.BackingStore
import tsec.authentication.{AuthenticatedCookie, Authenticator, BackingStore, SignedCookieAuthenticator, TSecAuthService, TSecCookieSettings}
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.concurrent.duration._

object Auth {
  type Cookie = AuthenticatedCookie[HMACSHA256, UserId]
  type SecuredService[F[_]] = TSecAuthService[User, Cookie, F]

  case class LoginRequest(login: String, password: String)

  val cookieSettings = TSecCookieSettings(
    cookieName = "tsec-auth",
    secure = false,
    expiryDuration = 10.minutes,
    maxIdle = Some(30.minutes),
    path = Some("/")
  )

  def createAuthenticator[F[_]: Async](identityStore: BackingStore[F, UserId, User]): Authenticator[F, UserId, User, Cookie] = {
//    val cookieStore: BackingStore[F, UUID, Cookie] = BackingStore.inMemory[F, UUID, Cookie](_.id)
    import scalacache.serialization.binary._
    val cookieStore: BackingStore[F, UUID, Cookie] = BackingStore.redis[F, UUID, Cookie](_.id)
    val key: MacSigningKey[HMACSHA256] = HMACSHA256.generateKey[Id]
    SignedCookieAuthenticator(cookieSettings, cookieStore, identityStore, key)
  }
}
