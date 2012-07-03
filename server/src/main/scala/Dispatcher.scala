package com.k2sw.scalanb

import client.ActorSpawner
import unfiltered.request._
import unfiltered.response._

import org.clapper.avsl.Logger
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import unfiltered.netty.websockets._
import akka.actor.{ActorRef, Props, ActorSystem}
import java.util.concurrent.atomic.AtomicInteger

/** unfiltered plan */
class Dispatcher(webSockPort:Int) {

  val logger = Logger(classOf[Dispatcher])

  val notbook_dir = new java.io.File( "." ).getCanonicalPath();
  val executionCounter = new AtomicInteger(0)

  val nbm = new NotebookManager()
  val system = ActorSystem("NotebookServer")
  val spawner = system.actorOf(Props[ActorSpawner])
  val km = new KernelManager

  val sessions = collection.mutable.Map[String, ActorRef]()
  def get(kernel: String) = {
    sessions.getOrElseUpdate(kernel, system.actorOf(Props(new Session(spawner)), name = "session"))
  }

  object WebSockets {

    val intent: unfiltered.netty.websockets.Plan.Intent =     {
      case req@Path(Seg("kernels" :: kernel :: channel :: Nil)) => {
          case Open(websock) =>
          logger.info("Opening Socket" + channel + " for " + kernel + " to " + websock)
          if (channel == "iopub")
            get(kernel) ! IopubChannel(new WebSockWrapper(websock))

        case Message(s, Text(msg)) =>
          logger.debug("Message for " + kernel + ":" + msg)

          val json = parse(msg)
          for {
            JField("header", header) <- json
            JField("session", session) <- header
            JField("msg_type", JString("execute_request")) <- header
            JField("content", content) <- json
            JField("code", JString(code)) <- content
          }  {
            val kern = get(kernel)
            val execCounter = executionCounter.incrementAndGet()
            kern ! SessionRequest(header, session, execCounter, code)

            val sock = new WebSockWrapper(s)
            sock.send(header, session, "execute_reply", ("execution_count" -> execCounter) ~ ("code" -> "1+2"))
          }

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
        nbm.save(id, nb)
        Ok
    }

    val otherIntent:unfiltered.netty.cycle.Plan.Intent = {
      case req@Path(Seg("clusters" :: Nil))  =>
        val s = """[{"profile":"default","status":"stopped","profile_dir":"C:\\Users\\Ken\\.ipython\\profile_default"}]"""
        JsonContent ~> ResponseString(s) ~> Ok

      case req@POST(Path(Seg("kernels" :: Nil)) & Params(params)) =>
        val kernelId = km.startKernel(params("notebook").head)
        val json = ("kernel_id" -> kernelId) ~ ("ws_url" -> "ws://127.0.0.1:%d".format(webSockPort))
        JsonContent ~> ResponseString(compact(render(json))) ~> Ok

      case req@Path(Seg(id :: Nil))  =>
        view(req, "notebook.ssp",
          "notebook_id" -> id,
          "project" -> nbm.notebookDir.getPath)

      case req@POST(Path(p) & Params(params)) =>
        println("post passing")
        Pass
    }

    def view[T](req: HttpRequest[T], file: String, extra: (String, Any)*) = {
      val Params(params) = req
      Scalate(req, "templates/" + file, (params.toSeq ++ extra): _*)
    }
  }
}