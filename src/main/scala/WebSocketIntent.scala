package com.k2sw.scalanb

import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import unfiltered.netty.websockets._
import unfiltered.request.{Seg, Path}

/**
 * User: Ken
 */

class WebSocketIntent(context: Context) {
  import context._

    val intent: unfiltered.netty.websockets.Plan.Intent = {
      case req@Path(Seg("kernels" :: kernel :: channel :: Nil)) => {
        case Open(websock) =>
          println("Opening Socket" + channel + " for " + kernel + " to " + websock)
          if (channel == "iopub")
            get(kernel) ! IopubChannel(new WebSockWrapper(websock))

        case Message(s, Text(msg)) =>
          println("Message for " + kernel + ":" + msg)

          val json = parse(msg)
          for {
            JField("header", header) <- json
            JField("session", session) <- header
            JField("msg_type", JString("execute_request")) <- header
            JField("content", content) <- json
            JField("code", JString(code)) <- content
          } {
            val kern = get(kernel)
            val execCounter = kern.executionCounter.incrementAndGet()
            kern ! ExecuteRequest(header, session, execCounter, code)

            val sock = new WebSockWrapper(s)
            sock.send(header, session, "execute_reply", ("execution_count" -> execCounter) ~ ("code" -> "1+2"))
          }


        case Close(websock) =>
          println("Closing Socket " + websock)
          sessions -= kernel

        case Error(s, e) =>
          e.printStackTrace()
      }
    }
}
