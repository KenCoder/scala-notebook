/*
Copyright (c) 2012 Kenneth Vogel
Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.k2sw.scalanb

import org.clapper.avsl.Logger
import unfiltered.netty.websockets._
import unfiltered.request.Path
import java.net.URL
import akka.actor.Actor

/**
 * Author: Ken
 */


/**embedded server */
object Server {
  val logger = Logger(Server.getClass)

  def main(args: Array[String]) {
    println(System.getProperty("java.class.path"))

    val port = 8899

    val app: App = new App(port)

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
