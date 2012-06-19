package com.k2sw.scalanb

import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import java.io._
import org.apache.commons.io.FileUtils
import java.util.{Date, UUID}
import java.text.SimpleDateFormat
import com.k2sw.scalanb.NBSerializer.{Metadata, Worksheet, Notebook}

/**
 * Author: Ken
 */

class NotebookManager {

  val extension = ".snb"
  val notebookDir = new java.io.File(".").getCanonicalFile

  def listNotebooks = {
    val files = notebookDir.listFiles map {_.getName} filter {_.endsWith(extension)} toIndexedSeq
    val res = files.sorted map { fn => {
      val name = fn.substring(0, fn.length - extension.length)
      ("name" -> name) ~ ("notebook_id" -> notebookId(name))
    } }
    JArray(res.toList)
  }

  def notebookFile(name: String) = new File(notebookDir.getCanonicalPath + File.separator + name + extension)

  def incrementFileName(base:String) = {
    Stream.from(1) map { i => base + i } filterNot { fn => notebookFile(fn).exists() } head
  }

  def newNotebook = {
    val name = incrementFileName("Untitled")
    val nb = Notebook( Metadata(name), List(Worksheet(Nil)), None)
    val id = notebookId(name)
    save(id, nb)
  }

  def getNotebook(id: String) = {
    val nb = load(idToName(id))
    val data = FileUtils.readFileToString(notebookFile(nb.name))
    val df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z'('Z')'");
    val last_mtime = df.format(new Date(notebookFile(nb.name).lastModified()))
    (last_mtime, nb.name, data)
  }



  def save(id: String, nb: Notebook) {
    FileUtils.writeStringToFile(notebookFile(nb.name), NBSerializer.write(nb))
    // If there was an old file that's different, then delete it because this is a rename
    for (oldName <- idToName.get(id)) {
      if (notebookFile(nb.name).compareTo(notebookFile(oldName)) != 0)
        notebookFile(oldName).delete()
    }
    associateIdToName(id, nb.name)
  }

  def load(name: String): Notebook = NBSerializer.read(FileUtils.readFileToString(notebookFile(name)))

  private def associateIdToName(id: String, name:String) {
    idToName.put(id, name)
    nameToId.put(name, id)
  }
  val nameToId = collection.mutable.Map[String, String]()
  val idToName = collection.mutable.Map[String, String]()

  def notebookId(name: String) = nameToId.getOrElse(name, {
    val id = UUID.randomUUID.toString
    associateIdToName(id, name)
    id
  })
}
