package com.k2sw.scalanb.client

import tools.nsc.Settings
import tools.nsc.interpreter.IMain
import java.io.{PrintWriter, ByteArrayOutputStream}
import tools.nsc.interpreter.Results.Success
import akka.actor.{ActorRef, Actor}

/**
 * Author: Ken
 *   Session - sits
 */


case class ExecuteRequest(code: String)

case class ExecuteResponse(stdout: String)

case class ErrorResponse(message: String)

case object InterruptRequest


class ScalaKernel extends Actor {
  lazy val stdoutBytes = new ByteArrayOutputStream()
  lazy val stdout = new PrintWriter(stdoutBytes)

  lazy val interp = {
    val settings = new Settings
    settings.embeddedDefaults[ScalaKernel]
    settings.usejavacp.tryToSetFromPropertyValue("true")
    val i = new IMain(settings, stdout)
    i.initializeSynchronous()
    i
  }

  def receive() = {
    case ExecuteRequest(code) =>
      val res = interp.interpret(code)
      stdout.flush()
      if (res == Success)
        sender ! ExecuteResponse(stdoutBytes.toString)
      else
        sender ! Seq(stdoutBytes.toString)
      stdoutBytes.reset()
  }
}