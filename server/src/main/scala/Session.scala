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
package com.k2sw.scalanb

import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._
import client.{ErrorResponse, ExecuteResponse, ExecuteRequest, NotebookKernel}
import unfiltered.netty.websockets.WebSocket
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import collection.mutable.Queue
import java.util.concurrent.atomic.AtomicInteger
import java.io.{ByteArrayOutputStream, PrintWriter}
import tools.nsc.interpreter.IMain
import tools.nsc.Settings
import tools.nsc.interpreter.Results.Success

/**
 * Author: Ken
 */

class Session extends Actor {

  private var iopub: Option[WebSockWrapper] = None
  private val requests = Queue[SessionRequest]()

  val executingRequests = Queue[SessionRequest]()

  lazy val kernel = context.actorOf(Props[NotebookKernel], name = "kernel")

  def checkRequest() {
    for {
      pub <- iopub
      req@SessionRequest(header, session, counter, code) <- requests
    } {
      pub.send( header, session, "status", ("execution_state" -> "busy"))
      pub.send( header, session, "pyin", ("execution_count" -> counter) ~ ("code" -> code))

      executingRequests += req
      kernel ! ExecuteRequest(code)
    }
    requests.clear()
  }

  def receive =  {
        case IopubChannel(sock) =>
          iopub = Some(sock)
          checkRequest()

        case e:SessionRequest =>
          requests += e
          checkRequest()

        case ExecuteResponse(msg) =>
          val SessionRequest(header, session, counter, _) = executingRequests.dequeue()
          iopub.get.send( header, session, "pyout", ("execution_count" -> counter) ~ ("data" -> ("text/plain" -> msg)))
          iopub.get.send( header, session, "status", ("execution_state" -> "idle"))

        case ErrorResponse(msg) =>
          val SessionRequest(header, session, _, _) = executingRequests.dequeue()
          iopub.get.send( header, session, "pyerr", ("status" -> "error") ~ ("ename" -> "Error") ~ ("traceback" -> Seq(msg)))
          iopub.get.send( header, session, "status", ("execution_state" -> "idle"))
  }
}


case class SessionRequest(header: JValue, session: JValue, counter: Int, code: String)
case class IopubChannel(sock: WebSockWrapper)
