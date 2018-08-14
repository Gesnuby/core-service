package org.gesnuby.vetclinic.repository.interpreter

import cats._
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits.UuidType
import doobie.util.log.LogHandler
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.repository.algebra.UserRepository

class DoobieUserRepository[F[_]: Monad](val xa: Transactor[F]) extends UserRepository[F] {

//  implicit val logHandler: LogHandler = LogHandler.jdkLogHandler

  def all: fs2.Stream[F, User] = {
    val query = sql"""
      select id, login, password, email
      from users
    """.query[User]
    query.stream.transact(xa)
  }

  def get(userId: UserId): F[Option[User]] = {
    val query = sql"""
      select id, login, password, email
      from users
      where id = $userId
    """.query[User]
    query.option.transact(xa)
  }

  def create(user: User): F[User] = {
    val query = sql"""
      insert into users (id, login, password, email)
      values (${user.id}, ${user.login}, ${user.password}, ${user.email})
    """.update
    query.withUniqueGeneratedKeys[User]("id", "login", "password", "email").transact(xa)
  }

  def update(user: User): F[Option[User]] = {
    val query = sql"""
      update users
      set login = ${user.login}, password = ${user.password}, email = ${user.email}
      where id = ${user.id}
    """.update
    query.run.transact(xa).map {
      case 1 => Some(user)
      case _ => None
    }
  }

  def delete(userId: UserId): F[Option[UserId]] = {
    val query = sql"""
      delete from users
      where id = $userId
    """.update
    query.run.transact(xa).map {
      case 1 => Some(userId)
      case _ => None
    }
  }

  def findByLogin(login: String): F[Option[User]] = {
    val query = sql"""
      select id, login, password, email
      from users
      where login = $login
    """.query[User]
    query.option.transact(xa)
  }
}

object DoobieUserRepository {
  def apply[F[_]: Monad](xa: Transactor[F]): DoobieUserRepository[F] = new DoobieUserRepository[F](xa)
}