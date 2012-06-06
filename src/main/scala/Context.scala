package com.k2sw.scalanb

import org.clapper.avsl.Logger
import unfiltered.request.{Params, HttpRequest}

/**
 * User: Ken
 */

class Context(val port:Int) {
  val logger = Logger(classOf[OtherServer])

  val notbook_dir = new java.io.File(".").getCanonicalPath();

  val nbm = new NotebookManager()

  val km = new KernelManager

  val sessions = collection.mutable.Map[String, Session]()

  def get(kernel: String) = {
    sessions.getOrElseUpdate(kernel, {
      val actor = new Session
      actor.start()
      actor
    })
  }
  def view[T](req: HttpRequest[T], file: String, extra: (String, Any)*) = {
    val Params(params) = req
    Scalate(req, file, (params.toSeq ++ extra): _*)
  }

}
