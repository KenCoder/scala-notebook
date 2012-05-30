package com.k2sw.scalanb

import actors.Actor
import unfiltered.netty.websockets.WebSocket

/**
 * Author: Ken
 */

class Session(val iopub: WebSocket) extends Actor {

  def act() {
    loop {
      react {
        case ExecuteRequest(ptr, code) =>
          send(skern.iopub, build( header, session, "status", ("execution_state" -> "busy")))
          send(skern.iopub, build( header, session, "pyin", ("execution_count" -> 1) ~ ("code" -> "1+2")))
          send(skern.iopub, build( header, session, "pyout", ("execution_count" -> 1) ~ ("data" -> ("text/plain" -> "4"))))
          send(skern.iopub, build( header, session, "status", ("execution_state" -> "idle")))
      }
    }
  }
}


case class ExecuteRequest(pointer: BigInt, code: String)
