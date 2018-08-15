package org.gesnuby.vetclinic.repository

import cats.effect.{IO, Sync}
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, PostgreSQLContainer}
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import org.gesnuby.vetclinic.DBConfig
import org.gesnuby.vetclinic.model.User
import org.gesnuby.vetclinic.model.User.UserId
import org.gesnuby.vetclinic.repository.algebra.UserRepository
import org.gesnuby.vetclinic.repository.interpreter.{DoobieUserRepository, InMemoryUserRepository}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuite, Matchers}

trait UserRepositoryTest extends FunSuite with Matchers with BeforeAndAfter {

  // UserRepository implementation name
  def repoName: String

  // UserRepository implementation to test
  def repo: UserRepository[IO]

  // Save user to the underlying storage
  def save(user: User): IO[Unit]

  // Save seq of users to the underlying storage
  def saveAll(users: List[User]): IO[Unit] = Sync[IO].delay {
    users.foreach(save)
  }

  // Clear underlying storage
  def clear: IO[Unit]

  val userF: IO[User] = User[IO]("user", "password", "test@email.ru")

  // Clear underlying storage before every test
  before {
    clear.unsafeRunSync()
  }

  test("all when there are no users") {
    val test = for {
      all <- repo.all.compile.toList
    } yield {
      all shouldBe empty
    }
    test.unsafeRunSync()
  }

  test("all when there are users") {
    import cats.implicits._
    val usersF = List(
      User[IO]("user1", "password1", "test1@email.ru"),
      User[IO]("user2", "password2", "test2@email.ru"),
      User[IO]("user3", "password3", "test3@email.ru")
    ).sequence
    val test = for {
      users <- usersF
      _ <- saveAll(users)
      all <- repo.all.compile.toList
    } yield {
      all should contain theSameElementsAs users
    }
    test.unsafeRunSync()
  }

  test("get when user exists") {
    val test = for {
      user <- userF
      _ <- save(user)
      maybeUser <- repo.get(user.id)
    } yield {
      maybeUser shouldBe defined
    }
    test.unsafeRunSync()
  }

  test("get when user doesn't exist") {
    val test = for {
      id <- User.uniqueId[IO]
      maybeUser <- repo.get(id)
    } yield {
      maybeUser shouldBe None
    }
    test.unsafeRunSync()
  }

  test("create") {
    val test = for {
      user <- userF
      savedUser <- repo.create(user)
    } yield {
      savedUser shouldBe user
    }
    test.unsafeRunSync()
  }

  test("update when user exists") {
    val test = for {
      user <- userF
      updatedUser = user.copy(password = "new_password", email = "test@yahoo.com")
      _ <- save(updatedUser)
      maybeUser <- repo.update(updatedUser)
    } yield {
      maybeUser shouldBe Some(updatedUser)
    }
    test.unsafeRunSync()
  }

  test("update when user doesn't exist") {
    val test = for {
      user <- userF
      maybeUser <- repo.update(user)
    } yield {
      maybeUser shouldBe None
    }
    test.unsafeRunSync()
  }

  test("delete when user exists") {
    val test = for {
      user <- userF
      _ <- save(user)
      maybeUserId <- repo.delete(user.id)
    } yield {
      maybeUserId shouldBe defined
    }
    test.unsafeRunSync()
  }

  test("delete when user doesn't exist") {
    val test = for {
      id <- User.uniqueId[IO]
      maybeUserId <- repo.delete(id)
    } yield {
      maybeUserId shouldBe None
    }
    test.unsafeRunSync()
  }

  test("findByLogin when user exists") {
    val test = for {
      user <- userF
      _ <- save(user)
      maybeUser <- repo.findByLogin(user.login)
    } yield {
      maybeUser shouldBe defined
    }
    test.unsafeRunSync()
  }

  test("findByLogin when user doesn't exist") {
    val test = for {
      maybeUser <- repo.findByLogin("some_login")
    } yield {
      maybeUser shouldBe None
    }
    test.unsafeRunSync()
  }
}

/**
  * InMemoryUserRepository implementation
  */
class InMemoryUserRepositoryTest extends UserRepositoryTest {
  private val store = new InMemoryKVStore[IO, UserId, User]
  def repoName: String = "InMemoryUserRepository"
  def repo: UserRepository[IO] = InMemoryUserRepository[IO](store)
  def save(user: User): IO[Unit] = store.put(user.id, user).map(_ => ())
  def clear: IO[Unit] = store.clear
}

/**
  * DoobieUserRepository implementation
  *
  * Tests would run inside postgres docker container
  */
class DoobieUserRepositoryTest extends UserRepositoryTest with ForAllTestContainer with BeforeAndAfterAll {
  import cats.implicits._
  import doobie.implicits._
  import doobie.postgres.implicits.UuidType

  private val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:10-alpine")
  override val container: Container = postgres

  private lazy val xa: Transactor[IO] = {
    val transactor = for {
      config <- DBConfig(postgres.driverClassName, postgres.jdbcUrl, postgres.username, postgres.password).pure[IO]
      _ <- FlywayDB[IO](config).migrate
      xa <- DoobieTransactor[IO](config).pure[IO]
    } yield xa
    transactor.unsafeRunSync()
  }

  def repoName: String = "DoobieUserRepository"
  def repo: UserRepository[IO] = DoobieUserRepository[IO](xa)
  def save(user: User): IO[Unit] = sql"""
      insert into users values (${user.id}, ${user.login}, ${user.password}, ${user.email})
    """.update.run.transact(xa).map(_ => ())
  override def saveAll(users: List[User]): IO[Unit] =
    Update[User]("insert into users (id, login, password, email) values (?, ?, ?, ?)").updateMany(users).transact(xa).map(_ => ())
  def clear: IO[Unit] =
    sql"""truncate users""".update.run.transact(xa).map(_ => ())
}