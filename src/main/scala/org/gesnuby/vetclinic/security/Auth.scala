package org.gesnuby.vetclinic.security

import java.util.UUID

import cats.effect.Async
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.repository.BackingStore
import scalacache.Cache
import tsec.authentication.{AuthenticatedCookie, Authenticator, BackingStore, SecuredRequestHandler, SignedCookieAuthenticator, TSecAuthService, TSecCookieSettings}
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.concurrent.duration._

object Auth {
  type Cookie = AuthenticatedCookie[HMACSHA256, UserId]
  type SecuredService[F[_]] = TSecAuthService[User, Cookie, F]
  type SecuredHandler[F[_]] = SecuredRequestHandler[F, UserId, User, Cookie]
  type AppAuthenticator[F[_]] = Authenticator[F, UserId, User, Cookie]

  private val cookieSettings = TSecCookieSettings(
    cookieName = "tsec-auth",
    secure = false,
    expiryDuration = 10.minutes,
    maxIdle = Some(30.minutes),
    path = Some("/")
  )

  def createAuthenticator[F[_]: Async](identityStore: BackingStore[F, UserId, User], cache: Cache[Cookie]): AppAuthenticator[F] = {
    import scalacache.serialization.binary._
    val cookieStore: BackingStore[F, UUID, Cookie] = BackingStore.cached[F, UUID, Cookie](_.id)(implicitly, implicitly, cache)
    val key: MacSigningKey[HMACSHA256] = HMACSHA256.buildKey("cookie-secret".getBytes)(HMACSHA256.idKeygenMac)
    SignedCookieAuthenticator(cookieSettings, cookieStore, identityStore, key)
  }
}
