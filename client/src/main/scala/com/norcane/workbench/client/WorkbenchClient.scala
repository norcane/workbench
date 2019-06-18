package com.norcane.workbench.client

import com.norcane.workbench.shared.{Api, ReadWrite}
import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.raw._
import upickle.default
import upickle.default.{Reader, Writer}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * The connection from workbench server to the client
  */
object Wire extends autowire.Server[ujson.Value, Reader, Writer] with ReadWrite {
  def wire(parsed: ujson.Arr): Unit = {
    val path :: (args: ujson.Obj) :: _ = parsed.value.toList
    val req = new Request(default.read[Seq[String]](path), args.value.toMap)
    Wire.route[Api](WorkbenchClient).apply(req)
  }
}

@JSExportTopLevel("WorkbenchClient")
object WorkbenchClient extends Api {
  @JSExport
  lazy val shadowBody: Node = dom.document.body.cloneNode(deep = true)
  @JSExport
  var interval: Int = 1000
  @JSExport
  var success: Boolean = false

  @JSExport
  def main(host: String, port: Int): Unit = {
    def rec(): Unit = {
      Ajax.post(s"http://$host:$port/notifications").onComplete {
        case util.Success(data) =>
          if (!success) println("\u2705 workBench: connected")
          success = true
          interval = 1000
          default
            .read[ujson.Arr](data.responseText)
            .value
            .foreach(v => Wire.wire(v.asInstanceOf[ujson.Arr]))
          rec()
        case util.Failure(cause) =>
          if (success) println(s"\uD83D\uDEAB workBench: disconnected (cause: $cause)")
          success = false
          interval = math.min(interval * 2, 30000)
          dom.window.setTimeout(() => rec(), interval)
      }
    }

    // Trigger shadowBody to get captured when the page first loads
    dom.window.addEventListener("load", (_: dom.Event) => {
      dom.console.log("\uD83D\uDEA7 workBench: loading...")
      shadowBody
      rec()
    })
  }

  @JSExport
  override def clear(): Unit = {
    dom.document.asInstanceOf[js.Dynamic].body = shadowBody.cloneNode(true)
    for (i <- 0 until 100000) {
      dom.window.clearTimeout(i)
      dom.window.clearInterval(i)
    }
  }

  @JSExport
  override def reload(): Unit = {
    dom.console.log("\uD83D\uDEA7 workBench: reloading website...")
    dom.window.location.reload()
  }

  @JSExport
  override def run(path: String): Unit = {
    val tag = dom.document.createElement("script").asInstanceOf[HTMLElement]
    tag.setAttribute("src", path)
    dom.document.head.appendChild(tag)
  }

  @JSExport
  override def print(level: String, msg: String): Unit = {
    level match {
      case "error" => dom.console.error(msg)
      case "warn"  => dom.console.warn(msg)
      case "info"  => dom.console.info(msg)
      case "log"   => dom.console.log(msg)
    }
  }
}
