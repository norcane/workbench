package com.norcane.workbench.plugin

import autowire._
import com.norcane.workbench.WorkbenchServer
import com.norcane.workbench.shared.Api
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.{LogEvent => Log4JLogEvent}
import org.apache.logging.log4j.message._
import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.Keys._
import sbt._

import scala.concurrent.ExecutionContext

object WorkbenchBasePlugin extends AutoPlugin {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  override def requires: ScalaJSPlugin.type = ScalaJSPlugin

  object autoImport {

    sealed trait StartMode
    object StartMode {
      case object OnCompile extends StartMode
      case object OnSbtLoad extends StartMode
      case object Manual extends StartMode
    }

    val localUrl = settingKey[(String, Int)]("localUrl")
    val workbenchDefaultRootObject =
      settingKey[Option[(String, String)]]("path to defaultRootObject served on `/` and rootDirectory")
    val workbenchCompression = settingKey[Boolean]("use gzip compression on HTTP responses")
    val workbenchStartMode = settingKey[StartMode](
      "should the web server start on sbt load, on compile, or only by manually running `startWorkbenchServer`")
    val startWorkbenchServer = taskKey[Unit]("start local web server manually")
  }
  import autoImport._

  val server = settingKey[WorkbenchServer]("local websocket server")

  private val workbenchSettings = Seq(
    localUrl := ("localhost", 12345),
    workbenchDefaultRootObject := None,
    workbenchCompression := false,
    (extraLoggers in ThisBuild) := {
      val clientLogger = new AbstractAppender("FakeAppender", null, PatternLayout.createDefaultLayout()) {
        override def append(event: Log4JLogEvent): Unit = {

          val level = sbt.internal.util.ConsoleAppender.toLevel(event.getLevel)
          val message = event.getMessage

          message match {
            case o: ObjectMessage =>
              o.getParameter match {
                case e: sbt.internal.util.StringEvent => server.value.Wire[Api].print(level.toString, e.message).call()
                case e: sbt.internal.util.ObjectEvent[_] =>
                  server.value.Wire[Api].print(level.toString, e.message.toString).call()
                case _ => server.value.Wire[Api].print(level.toString, message.getFormattedMessage).call()
              }
            case _ => server.value.Wire[Api].print(level.toString, message.getFormattedMessage).call()
          }
        }
      }
      clientLogger.start()
      val currentFunction = extraLoggers.value
      key: ScopedKey[_] =>
        clientLogger +: currentFunction(key)
    },
    server := {
      val server = new WorkbenchServer(localUrl.value._1,
                                       localUrl.value._2,
                                       workbenchDefaultRootObject.value.map(_._1),
                                       workbenchDefaultRootObject.value.map(_._2),
                                       workbenchCompression.value)
      if (workbenchStartMode.value == StartMode.OnSbtLoad) server.startServer()
      server
    },
    workbenchStartMode := StartMode.OnSbtLoad,
    startWorkbenchServer := server.value.startServer(),
    (compile in Compile) := (compile in Compile)
      .dependsOn(Def.task {
        if (workbenchStartMode.value == StartMode.OnCompile) server.value.startServer()
      })
      .value,
    (onUnload in Global) := {
      (onUnload in Global).value.compose { state =>
        server.value.kill()
        state
      }
    }
  )

  override def projectSettings: Seq[Setting[_]] = workbenchSettings
}
