package com.k2sw.scalanb

import unfiltered.response._

import org.clapper.avsl.Logger
import java.io.File
import unfiltered.request.Accepts.Jsonp
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import unfiltered.netty.websockets._
import unfiltered.netty.ReceivedMessage
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.jboss.netty.handler.codec.http.HttpHeaders
import java.util.UUID
import unfiltered.request._

/** unfiltered plan */
class OtherServer(context: Context) {
  import context._

  val otherIntent:unfiltered.netty.cycle.Plan.Intent = {
      case req@Path(Seg("clusters" :: Nil))  =>
        val s = """[{"profile":"default","status":"stopped","profile_dir":"C:\\Users\\Ken\\.ipython\\profile_default"}]"""
        JsonContent ~> ResponseString(s) ~> Ok

      case req@POST(Path(Seg("kernels" :: Nil)) & Params(params)) =>
        val kernelId = km.startKernel(params("notebook").head)
        println("Kernel id is " + kernelId)
        val json = ("kernel_id" -> kernelId) ~ ("ws_url" -> "ws://127.0.0.1:%d".format(port))
        JsonContent ~> ResponseString(compact(render(json))) ~> Ok

      case req@Path(Seg(id :: Nil))  =>
        view(req, "notebook.ssp",
          "notebook_id" -> id,
          "project" -> nbm.notebookDir.getPath)

      case req@POST(Path(p) & Params(params)) =>
        println("post passing")
        Pass
    }

}