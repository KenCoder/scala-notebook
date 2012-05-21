package com.k2sw.scalanb

import java.io.{FileFilter, File}
import java.util.UUID
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

/**
 * Author: Ken
 */

class NotebookManager {
  val extension = ".snb"
  val notebookDir = new java.io.File(".").getCanonicalPath

  def listNotebooks = {
    val files = new File(notebookDir).listFiles map {_.getName} filter {_.endsWith(extension)} toIndexedSeq
    val res = files.sorted map { fn => ("name" -> fn) ~ ("notebook_id" -> notebookId(fn)) }
    JArray(res.toList)
  }

  val knownIds = collection.mutable.Map[String, String]()

  def notebookId(name: String) = knownIds.getOrElseUpdate(name, UUID.randomUUID.toString)

}
