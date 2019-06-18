enablePlugins(ScalaJSPlugin)

// dynamic page reloading
enablePlugins(WorkbenchPlugin)

// (experimental feature) in-place code update with state preservation
// enablePlugins(WorkbenchSplicePlugin) // disable WorkbenchPlugin when activating

scalaVersion := "2.12.8"
name := "workbench-example"
version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.7"
)
