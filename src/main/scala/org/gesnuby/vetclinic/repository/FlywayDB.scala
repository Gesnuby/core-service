package org.gesnuby.vetclinic.repository

import cats.effect.Sync
import org.flywaydb.core.Flyway
import org.gesnuby.vetclinic.DBConfig

class FlywayDB[F[_]: Sync](config: DBConfig) {
  private val flyway: Flyway = new Flyway()
  flyway.setDataSource(
    config.url,
    config.username,
    config.password)
  flyway.setTable("migrations")

  def migrate: F[Unit] =
    Sync[F].delay(flyway.migrate())
}

object FlywayDB {
  def apply[F[_]: Sync](config: DBConfig): FlywayDB[F] = new FlywayDB[F](config)
}
