package com.k2sw.scalanb

import unfiltered.request._
import unfiltered.response._

import org.clapper.avsl.Logger
import java.io.File
import unfiltered.request.Accepts.Jsonp
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

/** unfiltered plan */
class App(port:Int) extends unfiltered.filter.Plan {
  import QParams._

  val logger = Logger(classOf[App])

  val notbook_dir = new java.io.File( "." ).getCanonicalPath();

  val nbm = new NotebookManager()

  val km = new KernelManager

  def intent = {
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
      val json = ("kernel_id" -> kernelId) ~ ("ws_url" -> "ws://127.0.0.1:%d".format(port))
      JsonContent ~> ResponseString(compact(render(json))) ~> Ok

    case req@Path(Seg(id :: Nil))  =>
      view(req, "notebook.ssp",
        "notebook_id" -> id,
        "project" -> nbm.notebookDir.getPath)

    case req@POST(Path(p) & Params(params)) =>
    Pass
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
