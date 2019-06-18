addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.28")

lazy val workbench = RootProject(file("../.."))
lazy val root = project.in(file(".")).dependsOn(workbench)
