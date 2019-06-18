package com.norcane.workbench.util

case class Take[T](instance: T) {
  def andThen(step: T => T): Take[T] = this.copy(instance = step(this.instance))
  def sideEffect(effect: T => Unit): Take[T] = { effect(instance); this }
  def get: T = instance
}
