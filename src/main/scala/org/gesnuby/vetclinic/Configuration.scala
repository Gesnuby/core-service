package org.gesnuby.vetclinic

import cats.effect.Sync
import cats.implicits._

final case class ServerConfig(host: String, port: Int)

object Configuration {

  import pureconfig._

  def load[F[_]](implicit F: Sync[F]): F[ServerConfig] =
    F.delay(loadConfig[ServerConfig]("server")).flatMap {
      case Right(config) => F.pure(config)
      case Left(errors) => F.raiseError(new Exception(errors.head.description))
    }
}
