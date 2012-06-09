package com.k2sw.scalanb.client

import akka.actor.Actor
import tools.nsc.Settings
import tools.nsc.interpreter.IMain
import java.io.{PrintWriter, ByteArrayOutputStream}
import tools.nsc.interpreter.Results.Success

/**
 * Author: Ken
 */


case class ExecuteRequest(code: String)

case class ExecuteResponse(stdout: String)

case class ErrorResponse(message: String)

case object InterruptRequest


class NotebookKernel extends Actor {
  lazy val stdoutBytes = new ByteArrayOutputStream()
  lazy val stdout = new PrintWriter(stdoutBytes)

  lazy val interp = {
    val settings = new Settings
    settings.embeddedDefaults[NotebookKernel]
    val i = new IMain(settings, stdout)
    i.initializeSynchronous()
    i
  }
  def receive = {
    case ExecuteRequest(code) =>
      stdout.flush()
      stdoutBytes.reset()
      val res = interp.interpret(code)
      stdout.flush()
      if (res == Success)
        sender ! ExecuteResponse(stdoutBytes.toString)
      else
        sender ! ErrorResponse(stdoutBytes.toString)
  }
}
