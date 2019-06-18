package com.norcane.workbench.shared

import ujson.Value
import upickle.default.{Reader, Writer}

/**
  * A standard way to read and write `Js.Value`s with autowire/upickle
  */
trait ReadWrite {
  def write[Result: Writer](r: Result): Value = upickle.default.writeJs(r)
  def read[Result: Reader](p: ujson.Value): Result = upickle.default.read[Result](p)
}

/**
  * Shared API between the workbench server and the workbench client,
  * comprising methods the server can call on the client to make it do
  * things
  */
trait Api {

  /**
    * Reset the HTML page to its initial state
    */
  def clear(): Unit

  /**
    * Reload the entire webpage
    */
  def reload(): Unit

  /**
    * Print a `msg` with the given logging `level`
    */
  def print(level: String, msg: String): Unit

  /**
    * Execute the javascript file available at the given `path`.
    */
  def run(path: String): Unit
}
