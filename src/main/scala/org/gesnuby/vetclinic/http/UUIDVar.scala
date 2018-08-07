package org.gesnuby.vetclinic.http

import java.util.UUID

import scala.util.Try

/**
  * UUID path extractor
  */
object UUIDVar {
  def unapply(s: String): Option[UUID] = {
    Try(UUID.fromString(s)).toOption
  }
}
