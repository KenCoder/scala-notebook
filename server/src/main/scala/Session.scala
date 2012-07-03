package com.k2sw.scalanb

import akka.actor._
import client._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import collection.mutable.Queue
import concurrent.ops._

/**
 * Author: Ken
 */

class Session(spawner: ActorRef) extends Actor {

  private var iopub: Option[WebSockWrapper] = None
  private var kernel: Option[ActorRef] = None

  private val requests = Queue[SessionRequest]()

  val executingRequests = Queue[SessionRequest]()

  def checkRequest() {
    for {
      pub <- iopub
      k <- kernel
    } {
      for (req@SessionRequest(header, session, counter, code) <- requests) {
        pub.send( header, session, "status", ("execution_state" -> "busy"))
        pub.send( header, session, "pyin", ("execution_count" -> counter) ~ ("code" -> code))

        executingRequests += req
        k ! ExecuteRequest(code)
      }
      requests.clear()
    }
  }

  def receive =  {
        case IopubChannel(sock) =>
          iopub = Some(sock)
          spawner ! SpawnActor(Props[ScalaKernel], "scalaKernel")
          checkRequest()

        case e:SessionRequest =>
          requests += e
          checkRequest()

        case ActorSpawned(id, ref) =>
          kernel = Some(ref)
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
