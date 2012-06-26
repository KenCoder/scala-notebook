package com.k2sw.scalanb

import unfiltered.netty.websockets.WebSocket
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.clapper.avsl.Logger

/**
 * Author: Ken
 */

class WebSockWrapper(val sock: WebSocket) {
  val logger = Logger(classOf[Dispatcher])
  def send(msg: String) {
    logger.debug("Sending " + msg)
    sock.send(msg)
  }

  def send(header: JValue, session: JValue, msgType: String, content: JValue) {
    val respJson = ("parent_header" -> header) ~
      ("msg_type" -> msgType) ~
      ("msg_id" -> UUID.randomUUID().toString) ~
      ("content" -> content) ~
      ("header" -> ("username" -> "kernel") ~
        ("session" -> session) ~
        ("msg_id" -> UUID.randomUUID().toString) ~
        ("msg_type" -> msgType))

    send(pretty(render(respJson)))
  }
}
