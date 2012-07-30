/*
Copyright (c) 2012 Kenneth Vogel
Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
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
