package com.k2sw.scalanb

import org.clapper.avsl.Logger
import unfiltered.netty.websockets._
import unfiltered.request.Path
import java.net.URL

/**
 * Author: Ken
 */


/**embedded server */
object Server {
  val logger = Logger(Server.getClass)

  def main(args: Array[String]) {
    val port = 8899

    val context = new Context(port)
    val app = new OtherServer(context)

    val wsPlan = unfiltered.netty.websockets.Planify (new WebSocketIntent(context).intent).onPass(_.sendUpstream(_))
    val pagePlan1 = unfiltered.netty.cycle.Planify(new NotebookServer(context).nbIntent)
    val pagePlan2 = unfiltered.netty.cycle.Planify(new OtherServer(context).otherIntent)
    val loggerPlan = unfiltered.netty.cycle.Planify(new ReqLogger().intent)

    val http = unfiltered.netty.Http(port)
    http.handler(wsPlan).handler(loggerPlan).handler(pagePlan1).handler(pagePlan2).resources(getClass().getResource("/from_ipython/")).run({
      svr =>
        unfiltered.util.Browser.open("http://127.0.0.1:%d".format(port))
    }, {
      svr =>
        logger.info("shutting down server")
    })
  }
}
