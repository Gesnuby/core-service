package org.gesnuby.vetclinic

import cats.effect.Sync
import cats.implicits._

final case class AppConfig(server: ServerConfig, db: DBConfig, redis: RedisConfig, security: SecurityConfig)
final case class ServerConfig(host: String, port: Int)
final case class DBConfig(driver: String, url: String, username: String, password: String)
final case class RedisConfig(host: String, port: Int)
final case class SecurityConfig(cookieSecret: String)

object Configuration {

  import pureconfig._

  def load[F[_]](implicit F: Sync[F]): F[AppConfig] =
    F.delay(loadConfig[AppConfig]("app")).flatMap {
      case Right(config) => F.pure(config)
      case Left(errors) => F.raiseError(new Exception(errors.head.description))
    }
}
