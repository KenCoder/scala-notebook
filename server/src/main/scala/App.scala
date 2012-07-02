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
import akka.actor.{ActorRef, Props, ActorSystem}
import java.util.concurrent.atomic.AtomicInteger
import client.{ExecuteRequest, CompletionRequest}

/** unfiltered plan */
class App(port:Int) {
  import QParams._

  val logger = Logger(classOf[App])

  val notbook_dir = new java.io.File( "." ).getCanonicalPath();
  val executionCounter = new AtomicInteger(0)

  val nbm = new NotebookManager()
  val system = ActorSystem("NotebookServer")
  // create the result listener, which will print the result and shutdown the system
  val km = new KernelManager

  def send(sock: WebSocket, msg: String) = {
    try {
      println("Sending " + msg)
      sock.send(msg)
    } catch {
      case e => e.printStackTrace()
    }
  }

  val sessions = collection.mutable.Map[String, ActorRef]()
  def get(kernel: String) = {
    sessions.synchronized {
      sessions.getOrElseUpdate(kernel, system.actorOf(Props[Session], name = "session_" + kernel))
    }
  }

  object WebSockets {

    val intent: unfiltered.netty.websockets.Plan.Intent =     {
      case req@Path(Seg("kernels" :: kernel :: channel :: Nil)) => {
        case Open(websock) =>
          println("Opening Socket " + channel + " for " + kernel + " to " + websock)

          if (channel == "iopub")
            get(kernel) ! IopubChannel(new WebSockWrapper(websock))
          else if (channel == "shell")
            get(kernel) ! ShellChannel(new WebSockWrapper(websock))

        case Message(socket, Text(msg)) =>
          println("Message for " + kernel + ":" + msg)

          val json = parse(msg)

          for {
            JField("header", header) <- json
            JField("session", session) <- header
            JField("msg_type", msgType) <- header
            JField("content", content) <- json
          } {
            msgType match {
              case JString("execute_request") => {
                for (JField("code", JString(code)) <- content) {
                  val kern = get(kernel)
                  val execCounter = executionCounter.incrementAndGet()
                  kern ! SessionRequest(header, session, ExecuteRequest(execCounter, code))
                }
              }

              case JString("complete_request") => {
                for (JField("line", JString(line)) <- content;
                     JField("cursor_pos", JInt(cursorPos)) <- content) {

                  val kern = get(kernel)
                  kern ! SessionRequest(header, session, CompletionRequest(line, cursorPos.toInt))
                }
              }
            }
          }

        case Close(websock) =>
          println("Closing Socket " + websock)
          // stop the actor
          val session = get(kernel)
          system.stop(session)
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

      case req@PUT(Path(Seg("notebooks" :: id :: Nil)))  =>
        val contents = Body.string(req)
        println("Putting notebook:" + contents)
        val nb  = NBSerializer.read( contents)
        nbm.save(nb)
        Ok
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

      case req@Path(Seg(id :: "print" :: Nil)) =>
        view(req, "printnotebook.ssp",
          "notebook_id" -> id,
          "project" -> nbm.notebookDir.getPath)

      case req@POST(Path(p) & Params(params)) =>
        println("post passing")
        Pass
    }

    def palindrome(s: String) = s.toLowerCase.reverse == s.toLowerCase
    def view[T](req: HttpRequest[T], file: String, extra: (String, Any)*) = {
      val Params(params) = req
//      Scalate(req, "templates/" + file, (params.toSeq ++ extra): _*)
      // CY: Hack becuase IntelliJ doesn't copy the resource dir into
      // the output folder the way SBT does
      Scalate(req, file, (params.toSeq ++ extra): _*)
    }
  }
}