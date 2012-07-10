package com.k2sw.scalanb.client

import akka.actor.Actor
import tools.nsc.Settings
import tools.nsc.interpreter.{HackIMain, JLineCompletion, Parsed}
import tools.nsc.interpreter.Completion.{ScalaCompleter, Candidates}
import java.io.{PrintWriter, ByteArrayOutputStream}
import tools.nsc.interpreter.Results.Success
import tools.nsc.interpreter.IMain

/**
 * Author: Ken
 */

trait KernelRequest

case class ExecuteRequest(counter: Int, code: String) extends KernelRequest
case object InterruptRequest extends KernelRequest
case class CompletionRequest(line: String, cursorPosition: Int) extends KernelRequest
case class ObjectInfoRequest(objName: String) extends KernelRequest

case class StreamResponse(data: String, name: String)

case class ExecuteResponse(stdout: String)

case class ErrorResponse(message: String)

// CY: With high probability, the matchedText field is the segment of the input line that could
// be sensibly replaced with (any of) the candidate.
// i.e.
//
// input: "abc".inst
//                  ^
// the completions would come back as List("instanceOf") and matchedText => "inst"
//
// ...maybe...
case class CompletionResponse(cursorPosition: Int, candidates: List[String], matchedText: String)

/*
  name
  call_def
  init_definition
  definition
  call_docstring
  init_docstring
  docstring
 */
case class ObjectInfoResponse(found: Boolean, name: String, callDef: String, callDocString: String)

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

  lazy val completion = new JLineCompletion(interp)

  def receive = {
    case ExecuteRequest(_, code) =>
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

    case CompletionRequest(line, cursorPosition) =>
      // CY: Don't ask to explain why this works.  Look at JLineCompletion.JLineTabCompletion.complete.mkDotted
      // The "regularCompletion" path is the only path that is (likely) to succeed
      // so we want access to that parsed version to pull out the part that was "matched"...
      // ...just...trust me.
      val parsed = Parsed.dotted(line, cursorPosition) // withVerbosity verbosity
      val matchedText = line.takeRight(cursorPosition - parsed.position)

      val completer = completion.completer().asInstanceOf[JLineCompletion#JLineTabCompletion]
      val Candidates(newCursor, candidates) = completer.complete(line, cursorPosition)

      sender ! CompletionResponse(cursorPosition, candidates, matchedText)

    case ObjectInfoRequest(objName) =>
      println(objName)
      val x = completion.completer().complete(objName + "\t\t\t", objName.length + 3)
      val y = completion.completer().complete(objName + "\t\t\t", objName.length + 3)
      sender ! ObjectInfoResponse(true, objName, "callDef", "callDocString")

  }
}
