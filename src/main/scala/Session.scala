package com.k2sw.scalanb

import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._
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
  private val requests = Queue[ExecuteRequest]()
  lazy val stdoutBytes = new ByteArrayOutputStream()
  lazy val stdout = new PrintWriter(stdoutBytes)
  lazy val interp = {
    val settings = new Settings
    settings.usejavacp.value = true
    val i = new IMain(settings, stdout)
    i.initializeSynchronous()
    i
  }

  def checkRequest() {
    for {
      pub <- iopub
      ExecuteRequest(header, session, counter, code) <- requests
    } {
      pub.send( header, session, "status", ("execution_state" -> "busy"))
      pub.send( header, session, "pyin", ("execution_count" -> counter) ~ ("code" -> code))
      val res = interp.interpret(code)
      stdout.flush()
      if (res == Success)
        pub.send( header, session, "pyout", ("execution_count" -> counter) ~ ("data" -> ("text/plain" -> stdoutBytes.toString)))
      else
        pub.send( header, session, "pyerr", ("status" -> "error") ~ ("ename" -> "Error") ~ ("traceback" -> Seq(stdoutBytes.toString)))

      stdoutBytes.reset()
      pub.send( header, session, "status", ("execution_state" -> "idle"))
    }
    requests.clear()
  }

  def receive =  {
        case IopubChannel(sock) =>
          iopub = Some(sock)
          checkRequest()

        case e:ExecuteRequest =>
          requests += e
          checkRequest()
  }
}


case class ExecuteRequest(header: JValue, session: JValue, counter: Int, code: String)
case class IopubChannel(sock: WebSockWrapper)
