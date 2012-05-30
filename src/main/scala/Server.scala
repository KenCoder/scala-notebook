package com.k2sw.scalanb

import org.clapper.avsl.Logger
import unfiltered.netty.websockets._
import unfiltered.request.Path

/**
 * Author: Ken
 */


/**embedded server */
object Server {
  val logger = Logger(Server.getClass)

  def main(args: Array[String]) {
    val port = 8899

    val app: App = new App(port)

    val wsPlan = unfiltered.netty.websockets.Planify (app.WebSockets.intent).onPass(_.sendUpstream(_))
    val pagePlan = unfiltered.netty.cycle.Planify(app.WebServer.intent)
    val loggerPlan = unfiltered.netty.cycle.Planify(new ReqLogger().intent)

    val http = unfiltered.netty.Http(port)
    http.handler(wsPlan).handler(loggerPlan).handler(pagePlan).run({
      svr =>
        unfiltered.util.Browser.open("http://127.0.0.1:%d".format(port))
    }, {
      svr =>
        logger.info("shutting down server")
    })
  }
}
