package com.norcane.workbench

import akka.actor.Actor
import com.norcane.workbench.util.Take

import scala.concurrent.Promise
import scala.concurrent.duration._

/**
  * Akka Actor responsible for handling polling requests from clients.
  */
class WorkbenchActor extends Actor {
  import WorkbenchActor.{Message, State}

  private val system = context.system
  import system.dispatcher

  system.scheduler.schedule(0.seconds, 10.seconds, self, Message.Clear)

  override def receive: Receive = process(State.Initial)

  private def process(currentState: State): Receive = {
    case m: Message =>
      m match {
        case Message.AddWaitingClient(promise) =>
          val newState = Take(currentState)
            .andThen(state => state.copy(waitingClients = promise :: state.waitingClients))
            .andThen(state => if (state.shouldRespond) respond(state) else state)
          context.become(process(newState.get))

        case Message.QueueMessage(message) =>
          val newState = Take(currentState)
            .andThen(state => state.copy(queuedMessages = message :: state.queuedMessages))
            .andThen(state => if (state.waitingClients.nonEmpty) respond(state) else state)
          context.become(process(newState.get))

        case Message.Clear =>
          context.become(process(respond(currentState)))

      }
  }

  private def respond(currentState: State): State = {
    val messages = ujson.Arr(currentState.queuedMessages: _*)
    Take(currentState)
      .sideEffect(_.waitingClients.foreach(_.success(messages)))
      .andThen(state => State.Initial.copy(numLastResponds = state.waitingClients.size))
      .get
  }
}

object WorkbenchActor {
  sealed trait Message
  object Message {
    case object Clear extends Message
    case class AddWaitingClient(promise: Promise[ujson.Arr]) extends Message
    case class QueueMessage(message: ujson.Arr) extends Message
  }

  case class State(waitingClients: List[Promise[ujson.Arr]], queuedMessages: List[ujson.Value], numLastResponds: Int) {
    def shouldRespond: Boolean =
      this.queuedMessages.nonEmpty && this.numLastResponds > 0 &&
        this.waitingClients.size >= this.numLastResponds
  }
  object State {
    val Initial: State = State(List.empty, List.empty, 0)
  }
}
