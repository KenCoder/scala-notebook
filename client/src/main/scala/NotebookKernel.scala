package com.k2sw.scalanb.client

import akka.actor.Actor
import tools.nsc.Settings
import tools.nsc.interpreter.HackIMain
import java.io.{PrintWriter, ByteArrayOutputStream}
import tools.nsc.interpreter.Results.Success
import tools.nsc.interpreter.IMain

/**
 * Author: Ken
 */


case class StreamResponse(data: String, name: String)

case class ExecuteRequest(code: String)

case class ExecuteResponse(stdout: String)

case class ErrorResponse(message: String)

case object InterruptRequest


class NotebookKernel extends Actor {

  lazy val stdoutBytes = new ByteArrayOutputStream() {
    override def write(i: Int): Unit = {
      // CY: Not used...
      sender ! StreamResponse(i.toString, "stdout")
      super.write(i)
    }

    override def write(bytes: Array[Byte]): Unit = {
      // CY: Not used...
      sender ! StreamResponse(bytes.toString, "stdout")
      super.write(bytes)
    }

    override def write(bytes: Array[Byte], off: Int, length: Int): Unit = {
      val data = new String(bytes, off, length)
      sender ! StreamResponse(data, "stdout")
      super.write(bytes, off, length)
    }
  }

  lazy val stdout = new PrintWriter(stdoutBytes)

  lazy val interp = {
    val settings = new Settings
    settings.embeddedDefaults[NotebookKernel]

    // CY: this is necessary, at least for me, for the repl to work when run from IntelliJ
    settings.usejavacp.value = true
    val i = new HackIMain(settings, stdout)
    i.initializeSynchronous()
    i
  }
  def receive = {
    case ExecuteRequest(code) =>
      stdout.flush()
      stdoutBytes.reset()
      // capture stdout if the code the user wrote was a println, for example
      val res = Console.withOut(stdoutBytes) {
        interp.interpret(code)
      }
      stdout.flush()
      if (res == Success) {
        val request = interp.previousRequests.last
        val handlers = request.handlers
        val last = handlers.last
        val evalValue = if (handlers.last.definesValue) {
          // CY: So for whatever reason, line.evalValue attemps to call the $eval method
          // on the class...a method that does not exist.  Not sure if this is a bug in the
          // REPL or some artifact of how we are calling it.
          val line = request.lineRep
          // a line like println(...) is technically a val, but returns null for some reason
          // so wrap it in an option in case that happens...
          val result = Option(line.call("$result"))
          result.map(_.toString)
        } else {
          None
        }

        val valueToSend = evalValue.getOrElse(stdoutBytes.toString)

        sender ! ExecuteResponse(valueToSend)
      } else
        sender ! ErrorResponse(stdoutBytes.toString)
  }
}
