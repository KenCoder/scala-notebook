package com.k2sw.scalanb

import org.clapper.avsl.Logger
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * Author: Ken
 */


/**embedded server */
object Server {
  // TODO: This should be somewhere in avsl
  FileUtils.forceMkdir(new File("logs"))

  val logger = Logger(Server.getClass)

  def main(args: Array[String]) {
    println(System.getProperty("java.class.path"))


    val port = 8899

    val app: Dispatcher = new Dispatcher(port)

    val wsPlan = unfiltered.netty.websockets.Planify (app.WebSockets.intent).onPass(_.sendUpstream(_))
    val pagePlan1 = unfiltered.netty.cycle.Planify(app.WebServer.nbIntent)
    val pagePlan2 = unfiltered.netty.cycle.Planify(app.WebServer.otherIntent)
    val loggerPlan = unfiltered.netty.cycle.Planify(new ReqLogger().intent)

    val http = unfiltered.netty.Http(port)
    http.handler(wsPlan).handler(loggerPlan).handler(pagePlan1).handler(pagePlan2).resources(getClass().getResource("/from_ipython/")).run({
      svr =>
        unfiltered.util.Browser.open("http://127.0.0.1:%d".format(port))
    }, {
      svr =>
        logger.info("shutting down server")
        app.system.shutdown()
    })
  }
}
