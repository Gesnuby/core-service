package org.gesnuby.vetclinic.repository

import cats.effect.Async
import doobie.util.transactor.Transactor
import org.gesnuby.vetclinic.DBConfig

class DoobieTransactor[F[_]: Async](db: DBConfig) {
  def transactor: Transactor[F] = Transactor.fromDriverManager[F](
    db.driver, db.url, db.username, db.password
  )
}

object DoobieTransactor {
  def apply[F[_]: Async](db: DBConfig): Transactor[F] = new DoobieTransactor[F](db).transactor
}
