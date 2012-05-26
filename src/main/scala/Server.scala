package com.k2sw.scalanb

import org.clapper.avsl.Logger

/**
 * Author: Ken
 */


/**embedded server */
object Server {
  val logger = Logger(Server.getClass)

  def main(args: Array[String]) {
    val port =8899
    val http = unfiltered.jetty.Http.local(port)
    http.context("/static") {
      _.resources(getClass().getResource("/from_ipython/static"))
    }.filter(new ReqLogger).filter(new App(port)).run({
      svr =>
        unfiltered.util.Browser.open(http.url)
    }, {
      svr =>
        logger.info("shutting down server")
    })
  }
}
