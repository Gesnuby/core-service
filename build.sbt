val http4sVersion = "0.18.15"
val logbackVersion = "1.2.3"
val pureConfigVersion = "0.9.1"
val tSecVersion = "0.0.1-M11"
val scalacacheVersion = "0.24.2"

lazy val core = (project in file("."))
  .settings(
    name := "core-service",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      // http4s
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      // https4s circe support
      "org.http4s" %% "http4s-circe" % http4sVersion,
      // security
      "io.github.jmcardon" %% "tsec-common" % tSecVersion,
      "io.github.jmcardon" %% "tsec-http4s" % tSecVersion,
      "de.mkammerer" % "argon2-jvm" % "2.4",
      // scalacache redis
      "com.github.cb372" %% "scalacache-redis" % scalacacheVersion,
      "com.github.cb372" %% "scalacache-cats-effect" % scalacacheVersion,
      "com.github.cb372" %% "scalacache-circe" % scalacacheVersion,
      // configuration
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      // logging
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      // swagger-ui
      "org.webjars" % "swagger-ui" % "3.17.4"
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-encoding", "utf-8",
      "-language:higherKinds",
      "-Ypartial-unification"
    ),
    mainClass in Compile := Some("org.gesnuby.vetclinic.App"),
    dockerBaseImage := "openjdk:jre-alpine"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)