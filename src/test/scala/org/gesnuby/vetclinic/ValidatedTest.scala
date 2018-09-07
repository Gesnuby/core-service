package org.gesnuby.vetclinic

import cats.data.Validated.{Invalid, Valid}
import cats.{Applicative, Functor, Semigroup, Semigroupal, Traverse}
import cats.data.{EitherT, NonEmptyList, Validated, ValidatedNel}
import cats.effect.{Async, IO, Sync}
import org.scalatest.FreeSpec
import cats.implicits._
import cats.effect.implicits._
import cats.syntax._
import fs2.Stream
import org.gesnuby.vetclinic.model.{UserError, UserAlreadyExists, UserNotFound}

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

    val fordCar = Car(1L, "Ford", Some("today")).pure[IO]
    val nel = fordCar.map { c =>
      c.validNel[String]
    }

    // F[ValidatedNel[String, User] => EitherT[F, NonEmptyList[String], User]
    // F[Validated[NonEmptyList[String], User] => EitherT[F, NonEmptyList[String], User]

    val left = Validated.condNel(true, "a", 1).pure[IO]
    val right = EitherT(left.map(_.toEither))

//    implicitly[Traverse[Future]]

    val s3 = Stream.eval {
      manufacturedShouldExist[IO](car)
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    val res = s2.merge(s3)

    val total: Stream[IO, ValidatedNel[String, Car]] = res.reduce((o1, o2) => (o1, o2).mapN((r1, r2) => r1))
    val valRes: IO[ValidatedNel[String, Car]] = total.compile.toList.map(_.head)

    sealed trait Err {
      def msg: String
    }
    case class SomeErr(v: String) extends Err {
      override def msg: String = v
    }

    case class Resp(msg: String)
    case class Good(v: Int)

    implicit val errSemigroup: Semigroup[Err] = Semigroup.instance[Err]((err1, err2) => SomeErr(s"${err1.msg}, ${err2.msg}"))

    val v1 = Validated.invalid[Err, Int](SomeErr("1"))
    val v2 = Validated.invalid[Err, Int](SomeErr("2"))
    val v3: Validated[Err, Int] = (v1, v2).mapN((s1, s2) => s1 + s2)
    println(v3)

    def test[F[_]: Functor: Semigroupal](
                fv1: F[ValidatedNel[Err, Good]],
                fv2: F[ValidatedNel[Err, Good]]): F[ValidatedNel[Err, Good]] = {
      val fv3: F[ValidatedNel[Err, Good]] = (fv1, fv2).mapN((vl1, vl2) => (vl1, vl2).mapN((v1v, v2v) => v1v))
      fv3
    }

    val fv1 = IO(Validated.invalidNel[Err, Good](SomeErr("1")))
    val fv2 = IO(Validated.invalidNel[Err, Good](SomeErr("2")))
    println(test[IO](fv1, fv2).unsafeRunSync)

    val vv1: Validated[String, Int] = Validated.invalid[String, Int]("1")
    val vv2: ValidatedNel[String, Int] = Validated.invalidNel[String, Int]("1")
    println(vv2.toEither)
  }

}
