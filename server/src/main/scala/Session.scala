package com.k2sw.scalanb

import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._
import client._
import client.ErrorResponse
import client.ExecuteRequest
import client.ExecuteResponse
import client.StreamResponse
import unfiltered.netty.websockets.WebSocket
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import collection.mutable.Queue
import java.util.concurrent.atomic.AtomicInteger
import java.io.{ByteArrayOutputStream, PrintWriter}
import tools.nsc.interpreter.IMain
import tools.nsc.Settings
import tools.nsc.interpreter.Results.Success
import com.k2sw.scalanb.SessionRequest
import scala.Some
import com.k2sw.scalanb.IopubChannel

/**
 * Author: Ken
 */

class Session extends Actor {

  private var iopub: Option[WebSockWrapper] = None
  private var shell: Option[WebSockWrapper] = None
  private val requests = Queue[SessionRequest]()

  val executingRequests = Queue[SessionRequest]()

  lazy val kernel = context.actorOf(Props[NotebookKernel], name = "kernel")

  def checkRequest() {
    for {
      pub <- iopub
      req@SessionRequest(header, session, kernelRequest) <- requests
    } {
      executingRequests += req

      kernelRequest match {
        case exec@ExecuteRequest(counter, code) =>
          pub.send( header, session, "status", ("execution_state" -> "busy"))
          pub.send( header, session, "pyin", ("execution_count" -> counter) ~ ("code" -> code))
          kernel ! exec

        case e =>
          kernel ! e
      }
    }

    requests.clear()
  }

  def receive = {
    case IopubChannel(sock) =>
      iopub = Some(sock)
      checkRequest()

    case ShellChannel(sock) =>
      shell = Some(sock)
      checkRequest()

    case e:SessionRequest =>
      requests += e
      checkRequest()

    case ExecuteResponse(msg) =>
      val SessionRequest(header, session, ExecuteRequest(counter, _)) = executingRequests.dequeue()
      iopub.get.send(header, session, "pyout", ("execution_count" -> counter) ~ ("data" -> ("text/html" -> msg)))
      iopub.get.send(header, session, "status", ("execution_state" -> "idle"))
      shell.get.send(header, session, "execute_reply", ("execution_count" -> counter))

    case StreamResponse(data, name) =>
      val SessionRequest(header, session, _) = executingRequests.front
      iopub.get.send(header, session, "stream", ("data" -> data) ~ ("name" -> name))

    case ErrorResponse(msg) =>
      val SessionRequest(header, session, ExecuteRequest(counter, _)) = executingRequests.dequeue()
      iopub.get.send( header, session, "pyerr", ("status" -> "error") ~ ("ename" -> "Error") ~ ("traceback" -> Seq(msg)))
      iopub.get.send( header, session, "status", ("execution_state" -> "idle"))
      shell.get.send(header, session, "execute_reply", ("execution_count" -> counter))

    case CompletionResponse(cursorPosition, candidates, matchedText) =>
      val SessionRequest(header, session, CompletionRequest(_, _)) = executingRequests.dequeue()
      shell.get.send(header, session, "complete_reply", ("matched_text" -> matchedText) ~ ("matches" -> candidates))

    case ObjectInfoResponse(found, name) =>
      val SessionRequest(header, session, ObjectInfoRequest(_)) = executingRequests.dequeue()
      shell.get.send(
        header,
        session,
        "object_info_reply",
        ("found" -> found) ~
        ("name" -> name))

  }
}


case class SessionRequest(header: JValue, session: JValue, kernelRequest: KernelRequest)
case class IopubChannel(sock: WebSockWrapper)
case class ShellChannel(sock: WebSockWrapper)
