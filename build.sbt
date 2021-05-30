import sbt._

val scala3Version = "3.0.0"

val circeVersion = "0.14.1"
val akkaVersion = "2.6.14"
val akkaHttpVersion = "10.2.4"
val mUnitVersion = "0.7.26"

lazy val circeDeps = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-jawn"
).map(_ % circeVersion)

lazy val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"       % akkaVersion,
  "com.typesafe.akka" %% "akka-http"        % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
).map(_.cross(CrossVersion.for3Use2_13))


lazy val root = project
  .in(file("."))
  .settings(
    name := "zicoin",
    version := "0.1.0",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq(
        "-deprecation",
        "-feature",
        "-unchecked",
        // "-language:strictEquality"
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++= 
      Seq(
        "org.scalameta" %% "munit" % mUnitVersion % Test,
        "ch.qos.logback" % "logback-classic" % "1.2.3",
        "com.typesafe" % "config" % "1.4.1"
      ) 
      ++ circeDeps
      ++ akkaDeps
  )
