package com.k2sw.scalanb

import org.clapper.avsl.Logger

/**
 * Author: Ken
 */


/**embedded server */
object Server {
  val logger = Logger(Server.getClass)

  def main(args: Array[String]) {
    val http = unfiltered.jetty.Http.anylocal
    http.context("/assets/static") {
      _.resources(getClass().getResource("/from_ipython/static"))
    }.filter(new ReqLogger).filter(new App).run({
      svr =>
        unfiltered.util.Browser.open(http.url)
    }, {
      svr =>
        logger.info("shutting down server")
    })
  }
}
