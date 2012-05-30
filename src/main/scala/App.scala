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

  def build(header: JValue, session: JValue, msgType: String, content: JValue) = {
    val respJson = ("parent_header" -> header) ~
      ("msg_type" -> msgType) ~
      ("msg_id" -> UUID.randomUUID().toString) ~
      ("content" -> content) ~
      ("header" -> ("username" -> "kernel") ~
              ("session" -> session) ~
              ("msg_id" -> UUID.randomUUID().toString) ~
              ("msg_type" -> msgType))

    pretty(render(respJson))
  }

  def send(sock: WebSocket, msg: String) = {
    try {
      println("Sending " + msg)
      sock.send(msg)
    } catch {
      case e => e.printStackTrace()
    }
  }

  var sockets = new scala.collection.mutable.ListBuffer[WebSocket]()
  object WebSockets {
    val sessions = collection.mutable.Map[String, Session]()

    val intent: unfiltered.netty.websockets.Plan.Intent =     {
      case req@Path(Seg("kernels" :: kernel :: channel :: Nil)) => {
        case Open(websock) =>
          println("Opening Socket" + channel + " for " + kernel + " to " + websock)
          if (channel == "iopub")
            sessions.getOrElseUpdate(kernel, {
              val actor = new Session(websock)
              actor.start()
              actor
            })

        case Message(s, Text(msg)) =>
          val skern = sessions(kernel)
          println("Message for " + kernel + ":" + msg)

          val json = parse(msg)
          for {
            JField("header", header) <- json
            JField("session", session) <- json
            JField("msg_type", JString("execute_request")) <- json
            JField("code", JString(code)) <- json
            JField("execution_count", JInt(execCount)) <- json
          }  {
            skern ! ExecuteRequest(execCount, code)
            }
          }

//          sessions(s) !? (SessionRequest(msg)) match {
//            case SessionResponse(txt) => s.send(txt)
//          }

        case Close(websock) =>
          println("Closing Socket " + websock)
          sessions -= kernel

        case Error(s, e) => println("Error %s" format e.getMessage)
      }
    }
  }

  object WebServer {
    val intent:unfiltered.netty.cycle.Plan.Intent = {
      case req@GET(Path("/")) =>
        view(req, "projectdashboard.ssp",
          "project" -> nbm.notebookDir.getPath)

      case Path(Seg("notebooks" :: Nil))  => Json(nbm.listNotebooks)

      case req@Path(Seg("new" :: Nil))  =>
        view(req, "notebook.ssp",
          "notebook_id" -> nbm.newNotebook,
          "project" -> nbm.notebookDir.getPath)

      case Path(Seg("notebooks" :: id :: Nil))  =>
        try {
          println("Looking for " + id)
          val (lastMod, name, data) = nbm.getNotebook(id)
          JsonContent ~> ResponseHeader("Content-Disposition", "attachment; filename=\"%s.scalanb".format(name) :: Nil) ~> ResponseHeader("Last-Modified", lastMod :: Nil) ~> ResponseString(data) ~> Ok
        } catch {
          case e:Exception => e.printStackTrace()
          throw e
        }
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
//
//      case msg =>
//      println("other passing: " + msg)
//      Pass

      //      val expected = for {
  //        int <- lookup("int") is
  //          int { _ + " is not an integer" } is
  //          required("missing int")
  //        word <- lookup("palindrome") is
  //          trimmed is
  //          nonempty("Palindrome is empty") is
  //          pred(palindrome, { _ + " is not a palindrome" }) is
  //          required("missing palindrome")
  //      } yield view(
  //        req, "palindrome.mustache",
  //        "body" -> "Yup. %d is an integer and %s is a palindrome".format(
  //          int.get, word.get
  //        )
  //      )
  //      expected(params) orFail { fails =>
  //        view(req, "palindrome.mustache", "errors" -> fails.map { f => Map("error" -> f.error) })
  //      }
    }

    def palindrome(s: String) = s.toLowerCase.reverse == s.toLowerCase
    def view[T](req: HttpRequest[T], file: String, extra: (String, Any)*) = {
      val Params(params) = req
      Scalate(req, file, (params.toSeq ++ extra): _*)
    }
  }
}