package com.k2sw.scalanb
import org.clapper.avsl.Logger
import unfiltered.request._
import unfiltered.response.Pass

/**
 * Author: Ken
 */

class ReqLogger extends unfiltered.filter.Plan {
    val logger = Logger(classOf[ReqLogger])
    def intent = {
      case req@GET(Path(p)) =>
        logger.info("Req " + p)
        Pass
      case req@POST(Path(p) & Params(params)) =>
        logger.debug("POST %s" format p)
        Pass
    }
}
