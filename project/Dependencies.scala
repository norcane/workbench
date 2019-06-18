import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object Dependencies {
  
  object Versions {
    val akkaHttp = "10.1.8"
    val akka = "2.5.23"
    val akkaStream = "2.5.23"
    val autowire = "0.2.6"
    val scalaJSDom = "0.9.7"
    val uPickle = "0.7.5"
    val uTest = "0.7.1"
  }

  // JVM dependencies
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp
  val akka = "com.typesafe.akka" %% "akka-actor" % Versions.akka
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akkaStream

  // JS and shared dependencies
  val autowire = Def.setting("com.lihaoyi" %%% "autowire" % Versions.autowire)
  val scalaJSDom = Def.setting("org.scala-js" %%% "scalajs-dom" % Versions.scalaJSDom)
  val uPickle = Def.setting("com.lihaoyi" %%% "upickle" % Versions.uPickle)

  // test dependencies
  val uTest = "com.lihaoyi" %% "utest" % Versions.uTest % "test"

}
