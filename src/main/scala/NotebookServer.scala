package com.k2sw.scalanb

import unfiltered.response._
import unfiltered.request._

/**
 * User: Ken
 */

class NotebookServer(context: Context) {
  import context._
    val nbIntent: unfiltered.netty.cycle.Plan.Intent = {
      case req@GET(Path("/")) =>
        view(req, "projectdashboard.ssp",
          "project" -> nbm.notebookDir.getPath)

      case Path(Seg("notebooks" :: Nil)) => Json(nbm.listNotebooks)

      case req@Path(Seg("new" :: Nil)) =>
        view(req, "notebook.ssp",
          "notebook_id" -> nbm.newNotebook,
          "project" -> nbm.notebookDir.getPath)

      case GET(Path(Seg("notebooks" :: id :: Nil))) =>
        try {
          println("Looking for " + id)
          val (lastMod, name, data) = nbm.getNotebook(id)
          JsonContent ~> ResponseHeader("Content-Disposition", "attachment; filename=\"%s.scalanb".format(name) :: Nil) ~> ResponseHeader("Last-Modified", lastMod :: Nil) ~> ResponseString(data) ~> Ok
        } catch {
          case e: Exception => e.printStackTrace()
          throw e
        }


      case req@PUT(Path(Seg("notebooks" :: id :: Nil))) =>
        val contents = Body.string(req)
        println("Putting notebook:" + contents)
        val nb = NBSerializer.read(contents)
        nbm.save(nb)
        Ok

      case req@DELETE(Path(Seg("notebooks" :: id :: Nil))) =>
        println("Deleting notebook:" + id)
        nbm.delete(id)
        Ok
    }
  }
