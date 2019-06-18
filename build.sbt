import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbtcrossproject.CrossType

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.norcane"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / homepage := Some(url("https://github.com/norcane/workbench"))

ThisBuild / developers := List(
  Developer(
    id = "vaclav.svejcar",
    name = "Vaclav Svejcar",
    email = "vaclav.svejcar@gmail.com",
    url = url("https://github.com/vaclavsvejcar")
  )
)

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/norcane/workbench"),
    "scm:git@github.com:norcane/workbench.git"
  )
)

// Bintray configuration
ThisBuild / bintrayOrganization := Some("norcane")
ThisBuild / bintrayRepository := "workbench"

lazy val root = project.in(file("."))
  .settings(
    name := "workbench",
    sbtPlugin := true,
    publishArtifact in Test := false,
    (resources in Compile) += {
      (fullOptJS in(client, Compile)).value
      (artifactPath in(client, Compile, fullOptJS)).value
    },
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.28"),
    libraryDependencies ++= Seq(
      Dependencies.akkaHttp,
      Dependencies.akka,
      Dependencies.akkaStream,
      Dependencies.autowire.value,
      Dependencies.uPickle.value,
      Dependencies.uTest
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .dependsOn(sharedJVM)
  .aggregate(sharedJVM, sharedJS)

lazy val client = project.in(file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.autowire.value,
      Dependencies.scalaJSDom.value,
      Dependencies.uPickle.value
    ),
    emitSourceMaps := false
  )
  .dependsOn(sharedJS)

lazy val shared = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("shared"))
  .settings(
    name := "workbench-shared",
    libraryDependencies ++= Seq(
      Dependencies.uPickle.value
    )
  )
lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

ThisBuild / scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8", // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
)