import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0"

  val AkkaVersion = "2.6.8"
  val AkkaHttpVersion = "10.2.0"
  lazy val akka = "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaVersion
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion

  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
}
