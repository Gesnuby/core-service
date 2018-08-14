package org.gesnuby.vetclinic

import cats.data.Validated.{Invalid, Valid}
import cats.{Applicative, Functor, Semigroupal, Traverse}
import cats.data.{Validated, ValidatedNel}
import cats.effect.{Async, IO, Sync}
import org.scalatest.FreeSpec
import cats.implicits._
import cats.effect.implicits._
import cats.syntax._
import fs2.Stream

import scala.concurrent.Future

class ValidatedTest extends FreeSpec {

  case class Car(id: Long, model: String, manufactured: Option[String])

  def modelShouldMatch[F[_]: Sync](modelName: String, car: Car): F[ValidatedNel[String, Car]] = Sync[F].delay {
    println("check 1")
    Validated.condNel(car.model == modelName, car, "car model didn't match")
  }

  def manufacturedShouldExist[F[_]: Sync](car: Car): F[ValidatedNel[String, Car]] = Sync[F].delay {
    println("check 2")
    Validated.condNel(car.manufactured.isDefined, car, "manufactures must be filled")
  }

  "Car should be Ford with specified manufactured year" in {
    val car = Car(1L, "Audi", None)

//    val stream: Stream[IO, ValidatedNel[String, Car]] =

    val s2 = Stream.eval {
      modelShouldMatch[IO]("Ford", car)
    }

    val validations: List[IO[ValidatedNel[String, Car]]] = List(
      modelShouldMatch[IO]("Ford", car),
      manufacturedShouldExist[IO](car)
    )

//    val ios: IO[List[ValidatedNel[String, Int]]] = List(IO(1.validNel[String]), IO("2".invalidNel[Int])).sequence
    val ios: IO[List[ValidatedNel[String, Car]]] = validations.sequence
    val red: IO[ValidatedNel[String, Unit]] = ios.map(ll => ll.sequence_)
    println(red.unsafeRunSync)

    println(red.unsafeRunSync.toEither)

//    implicitly[Traverse[Future]]

    val s3 = Stream.eval {
      manufacturedShouldExist[IO](car)
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    val res = s2.merge(s3)

    val total: Stream[IO, ValidatedNel[String, Car]] = res.reduce((o1, o2) => (o1, o2).mapN((r1, r2) => r1))
    val valRes: IO[ValidatedNel[String, Car]] = total.compile.toList.map(_.head)
//    println(valRes.unsafeRunSync())
  }

}
