import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0"

  val AkkaVersion = "2.6.8"
  lazy val akka = "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
}
