import Dependencies._

ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val commonSettings = Seq(
  scalacOptions ++= "-deprecation" :: "-feature" :: "-Xlint" :: Nil,
  scalacOptions in (Compile, console) ~= {_.filterNot(_ == "-Xlint")},
  scalafmtOnCompile := true
)

lazy val root = (project in file("."))
  .settings(
    name := "akka-study",
    commonSettings
  )
  .aggregate(
    akka01,
    akka02,
    akka03,
    akka04
  )

lazy val akka01 = (project in file("akka01"))
  .settings(
    name := "akka-study-01",
    commonSettings,
    libraryDependencies += akka,
    libraryDependencies += logback,
    libraryDependencies += scalaTest % Test
  )

lazy val akka02 = (project in file("akka02"))
  .settings(
    name := "akka-study-02",
    commonSettings,
    libraryDependencies += akka,
    libraryDependencies += logback,
    libraryDependencies += scalaTest % Test
  )

lazy val akka03 = (project in file("akka03"))
  .settings(
    name := "akka-study-03",
    commonSettings,
    libraryDependencies += akka,
    libraryDependencies += logback,
    libraryDependencies += scalaTest % Test
  )

lazy val akka04 = (project in file("akka04"))
  .settings(
    name := "akka-study-04",
    commonSettings,
    libraryDependencies += akka,
    libraryDependencies += akkaStream,
    libraryDependencies += akkaHttp,
    libraryDependencies += logback,
    libraryDependencies += scalaTest % Test
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
