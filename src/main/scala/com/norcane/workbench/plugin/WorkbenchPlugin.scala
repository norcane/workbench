package com.norcane.workbench.plugin

import autowire._
import com.norcane.workbench.shared.Api
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.Keys._
import sbt._

import scala.concurrent.ExecutionContext

object WorkbenchPlugin extends AutoPlugin {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  override def requires: WorkbenchBasePlugin.type = WorkbenchBasePlugin

  object autoImport {
    val refreshBrowsers = taskKey[Unit]("Sends a message to all connected web pages asking them to refresh the page")
  }
  import ScalaJSPlugin.AutoImport._
  import WorkbenchBasePlugin.server
  import autoImport._

  private val workbenchSettings = Seq(
    refreshBrowsers := {
      streams.value.log.info("workBench: reloading website")
      server.value.Wire[Api].reload().call()
    },
    refreshBrowsers := refreshBrowsers.triggeredBy(fastOptJS in Compile).value
  )

  override def projectSettings: Seq[Setting[_]] = workbenchSettings

}
