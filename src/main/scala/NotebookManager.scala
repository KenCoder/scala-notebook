package com.k2sw.scalanb

import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import java.io._
import org.apache.commons.io.FileUtils
import java.util.{Date, UUID}
import java.text.SimpleDateFormat

/**
 * Author: Ken
 */

class NotebookManager {

  val extension = ".snb"
  val notebookDir = new java.io.File(".").getCanonicalFile

  def listNotebooks = {
    val files = notebookDir.listFiles map {_.getName} filter {_.endsWith(extension)} toIndexedSeq
    val res = files.sorted map { fn => ("name" -> fn) ~ ("notebook_id" -> notebookId(fn)) }
    JArray(res.toList)
  }

  def notebookFile(name: String) = new File(notebookDir.getCanonicalPath + File.pathSeparator + name + extension)

  def incrementFileName(base:String) = {
    Stream.from(1) map { i => base + i } filterNot { fn => notebookFile(fn).exists() } head
  }

  def newNotebook = {
    val name = incrementFileName("Untitled")
    val nb = Notebook(name, Metadata(name), List(Worksheet(Nil)))
    save(nb)
    notebookId(name)
  }

  def getNotebook(id: String) = {
    val nb = load(idToName(id))
    val data = FileUtils.readFileToString(notebookFile(nb.name))
    val df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z'('Z')'");
    val last_mtime = df.format(new Date(notebookFile(nb.name).lastModified()))
    (last_mtime, nb.name, data)
  }



  def save(nb: Notebook) {
    FileUtils.writeStringToFile(notebookFile(nb.name), NBSerializer.write(nb))
  }

  def load(name: String): Notebook = NBSerializer.read(FileUtils.readFileToString(notebookFile(name)))

  val nameToId = collection.mutable.Map[String, String]()
  val idToName = collection.mutable.Map[String, String]()

  def notebookId(name: String) = nameToId.getOrElseUpdate(name, {
    val id = UUID.randomUUID.toString
    idToName += id -> name
    id
  })
}
