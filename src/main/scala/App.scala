package com.k2sw.scalanb

import unfiltered.request._
import unfiltered.response._

import org.clapper.avsl.Logger
import java.io.File
import unfiltered.request.Accepts.Jsonp

/** unfiltered plan */
class App extends unfiltered.filter.Plan {
  import QParams._

  val logger = Logger(classOf[App])

  val notbook_dir = new java.io.File( "." ).getCanonicalPath();

  val nbm = new NotebookManager()

  def intent = {
    case req@GET(Path("/")) =>
      view(req, "projectdashboard.ssp",
        "project" -> nbm.notebookDir.getPath)

    case GET(Path("/notebooks"))  => Json(nbm.listNotebooks)
    case req@GET(Path("/new"))  =>
      view(req, "notebook.ssp",
        "notebook_id" -> nbm.newNotebook,
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
