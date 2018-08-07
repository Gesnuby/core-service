package org.gesnuby.vetclinic.security

import de.mkammerer.argon2.Argon2Factory.Argon2Types

case class ArgonSettings(argon2Type: Argon2Types, iterations: Int, memory: Int, parallelism: Int)

object ArgonSettings {
  val default: ArgonSettings = new ArgonSettings(Argon2Types.ARGON2d, 2, 65536, 2)
}
