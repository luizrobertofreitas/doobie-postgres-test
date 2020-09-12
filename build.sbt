import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

scalacOptions += "-Ypartial-unification"

lazy val doobieVersion = "0.8.8"
lazy val circeVersion = "0.12.1"
lazy val catsVersion = "2.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "doobie-postgresql-test",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "org.tpolecat" %% "doobie-core"           % doobieVersion,
      "org.tpolecat" %% "doobie-postgres"       % doobieVersion,
      "org.tpolecat" %% "doobie-specs2"         % doobieVersion,
      "io.circe"     %% "circe-core"            % circeVersion,
      "io.circe"     %% "circe-generic"         % circeVersion,
      "io.circe"     %% "circe-generic-extras"  % circeVersion,
      "io.circe"     %% "circe-parser"          % circeVersion,
      "org.typelevel" %% "cats-core"            % catsVersion,
      "org.typelevel" %% "cats-effect"          % catsVersion
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
