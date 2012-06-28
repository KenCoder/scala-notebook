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
    settings.usejavacp.value = true
    val i = new HackIMain(settings, stdout)
    i.initializeSynchronous()
    i
  }
  def receive = {
    case ExecuteRequest(code) =>
      stdout.flush()
      stdoutBytes.reset()
      val res = interp.interpret(code, true)
      stdout.flush()
      if (res == Success) {
        val request = interp.previousRequests.last
        val line = request.lineRep
        // CY: So for whatever reason, line.evalValue attemps to call the $eval method
        // on the class...a method that does not exist.  Not sure if this is a bug in the
        // REPL or some artifact of how we are calling it.
        val foo = line.call("$result")
        println("tree: " + request.trees.mkString("\n"))
        println("eval value: " + request.lineRep.evalValue)
        println("eval class: " + request.lineRep.evalClass.getMethods.mkString("\n"))
//        println("evalCaught: " + request.lineRep.evalCaught.foreach(_.printStackTrace()))
        request.lineRep.evalCaught.foreach(_.printStackTrace())
        println("class: " + request.getEvalTyped.toString)
        println("Reuqest:" + request)
        println("Value of term: " + interp.valueOfTerm("res0"))
        val value = request.getEval.getOrElse("empty").toString
        println("Most recent var: " + value)
//        sender ! ExecuteResponse(stdoutBytes.toString)

        sender ! ExecuteResponse(foo.toString)
      } else
        sender ! ErrorResponse(stdoutBytes.toString)
  }
}
