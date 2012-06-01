package com.k2sw.scalanb

import unfiltered.request._
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

/** unfiltered plan */
class App(port:Int) {
  import QParams._

  val logger = Logger(classOf[App])

  val notbook_dir = new java.io.File( "." ).getCanonicalPath();

  val nbm = new NotebookManager()

  val km = new KernelManager

  def send(sock: WebSocket, msg: String) = {
    try {
      println("Sending " + msg)
      sock.send(msg)
    } catch {
      case e => e.printStackTrace()
    }
  }

  val sessions = collection.mutable.Map[String, Session]()
  def get(kernel: String) = {
    sessions.getOrElseUpdate(kernel, {
      val actor = new Session
      actor.start()
      actor
    })
  }

  object WebSockets {

    val intent: unfiltered.netty.websockets.Plan.Intent =     {
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
          }  {
            val kern = get(kernel)
            val execCounter = kern.executionCounter.incrementAndGet()
            kern ! ExecuteRequest(header, session, execCounter, code)

            val sock = new WebSockWrapper(s)
            sock.send(header, session, "execute_reply", ("execution_count" -> execCounter) ~ ("code" -> "1+2"))
          }

//          sessions(s) !? (SessionRequest(msg)) match {
//            case SessionResponse(txt) => s.send(txt)
//          }

        case Close(websock) =>
          println("Closing Socket " + websock)
          sessions -= kernel

        case Error(s, e) =>
          e.printStackTrace()
    }
    }
  }

  object WebServer {
    val nbIntent:unfiltered.netty.cycle.Plan.Intent = {
      case req@GET(Path("/")) =>
        view(req, "projectdashboard.ssp",
          "project" -> nbm.notebookDir.getPath)

      case Path(Seg("notebooks" :: Nil))  => Json(nbm.listNotebooks)

      case req@Path(Seg("new" :: Nil))  =>
        view(req, "notebook.ssp",
          "notebook_id" -> nbm.newNotebook,
          "project" -> nbm.notebookDir.getPath)

      case GET(Path(Seg("notebooks" :: id :: Nil)))  =>
        try {
          println("Looking for " + id)
          val (lastMod, name, data) = nbm.getNotebook(id)
          JsonContent ~> ResponseHeader("Content-Disposition", "attachment; filename=\"%s.scalanb".format(name) :: Nil) ~> ResponseHeader("Last-Modified", lastMod :: Nil) ~> ResponseString(data) ~> Ok
        } catch {
          case e:Exception => e.printStackTrace()
          throw e
        }

      case POST(Path(Seg("notebooks" :: id :: Nil)) & Params(params))  =>
        println("Posting notebook %s params %s".format(id, params))
        Pass
    }

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

    def palindrome(s: String) = s.toLowerCase.reverse == s.toLowerCase
    def view[T](req: HttpRequest[T], file: String, extra: (String, Any)*) = {
      val Params(params) = req
      Scalate(req, file, (params.toSeq ++ extra): _*)
    }
  }
}