package com.k2sw.scalanb
import org.clapper.avsl.Logger
import unfiltered.request._
import unfiltered.response.Pass

/**
 * Author: Ken
 */

class ReqLogger {
    val logger = Logger(classOf[ReqLogger])
    val intent:unfiltered.netty.cycle.Plan.Intent = {
      case req@GET(Path(p)) =>
        logger.info("Req " + req.uri)
        Pass
      case req@POST(Path(p) & Params(params)) =>
        logger.info("POST %s: %s".format(p, params))
        Pass
    }
}
