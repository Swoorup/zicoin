import sbt._

val scala3Version = "3.0.0-RC1"

val circeVersion = "0.14.0-M4"
val akkaVersion = "2.6.13"
val akkaHttpVersion = "10.2.4"
val mUnitVersion = "0.7.22"

lazy val circeDeps = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-jawn"
).map(_ % circeVersion)

lazy val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"       % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
).map(_.withDottyCompat(scala3Version))


lazy val root = project
  .in(file("."))
  .settings(
    name := "zicoin",
    version := "0.1.0",
    scalaVersion := scala3Version,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++= 
      Seq(
        "org.scalameta" %% "munit" % mUnitVersion % Test,
        "com.typesafe.play" %% "play-json" % "2.10.0-RC2", 
        "ch.qos.logback" % "logback-classic" % "1.2.3"
      ) 
      ++ circeDeps
      ++ akkaDeps
  )
