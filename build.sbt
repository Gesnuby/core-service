val http4sV = "0.18.16"
val logbackV = "1.2.3"
val pureConfigV = "0.9.1"
val tSecV = "0.0.1-M11"
val scalacacheV = "0.24.2"
val doobieV = "0.5.3"
val flywayV = "5.1.4"
val scalatestV = "3.0.5"
val scalacheckV = "1.14.0"
val testcontainersScalaV = "0.20.0"
val testcontainersPostgresV = "1.8.3"
val swaggerUIV = "3.17.6"
val zincV = "1.2.1"

lazy val core = (project in file("."))
  .settings(
    name := "core-service",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      // http4s
      "org.http4s" %% "http4s-dsl" % http4sV,
      "org.http4s" %% "http4s-blaze-server" % http4sV,
      // https4s circe support
      "org.http4s" %% "http4s-circe" % http4sV,
      // security
      "io.github.jmcardon" %% "tsec-common" % tSecV,
      "io.github.jmcardon" %% "tsec-http4s" % tSecV,
      "de.mkammerer" % "argon2-jvm" % "2.4",
      // database
      "org.tpolecat" %% "doobie-core" % doobieV,
      "org.tpolecat" %% "doobie-postgres" % doobieV,
      "org.tpolecat" %% "doobie-hikari" % doobieV,
      // database migrations
      "org.flywaydb" % "flyway-core" % flywayV,
      // validation
      "commons-validator" % "commons-validator" % "1.6",
      // scalacache
      "com.github.cb372" %% "scalacache-redis" % scalacacheV,
      "com.github.cb372" %% "scalacache-caffeine" % scalacacheV,
      "com.github.cb372" %% "scalacache-cats-effect" % scalacacheV,
      "com.github.cb372" %% "scalacache-circe" % scalacacheV,
      // configuration
      "com.github.pureconfig" %% "pureconfig" % pureConfigV,
      // logging
      "ch.qos.logback" % "logback-classic" % logbackV,
      // swagger-ui
      "org.webjars" % "swagger-ui" % swaggerUIV,
      // incremental compilation
      "org.scala-sbt" %% "zinc" % zincV,
      // testing
      "org.scalatest" %% "scalatest" % scalatestV % Test,
      "org.scalacheck" %% "scalacheck" % scalacheckV % Test,
      // database testing with testcontainers
      "com.dimafeng" %% "testcontainers-scala" % testcontainersScalaV % Test,
      "org.testcontainers" % "postgresql" % testcontainersPostgresV % Test
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-encoding", "utf-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-Ypartial-unification"
    ),
    mainClass in Compile := Some("org.gesnuby.vetclinic.App"),
    Test / fork := true,
    dockerBaseImage := "openjdk:jre-alpine"
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)