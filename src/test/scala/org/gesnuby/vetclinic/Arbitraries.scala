package org.gesnuby.vetclinic

import org.gesnuby.vetclinic.model.User
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

object Arbitraries {
  implicit val arbUser: Arbitrary[User] = Arbitrary[User] {
    for {
      id <- Gen.delay(Gen.const(User.uniqueId))
      login <- arbitrary[String].suchThat(_.nonEmpty)
      password <- arbitrary[String].suchThat(_.nonEmpty)
      email <- arbitrary[String]
    } yield User(id, login, password, email)
  }
}
