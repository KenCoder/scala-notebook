package com.k2sw.scalanb

import java.util.UUID
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import java.io._

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

  def notebookFile(filename: String) = new File(notebookDir.getPath + File.pathSeparator + filename + extension)

  def incrementFileName(base:String) = {
    Stream.from(1) map { i => base + i } filterNot { fn => notebookFile(fn).exists() } head
  }

  def newNotebook = {
    val name = incrementFileName("Untitled")

    val nb = new Notebook(notebookId(name), name)
    save(nb)
    nb.id
  }


  def save(nb: Notebook) {
    val oos = new ObjectOutputStream(new FileOutputStream(notebookFile(nb.name)))
    try {
      oos.writeObject(nb)
    }
    finally {
      oos.close()
    }
  }

  def load(name: String): Notebook = {
    val ois = new ObjectInputStream(new FileInputStream(notebookFile(name)))
    try {
      ois.readObject().asInstanceOf[Notebook]
    } finally {
      ois.close()
    }
  }



  val knownIds = collection.mutable.Map[String, String]()

  def notebookId(name: String) = knownIds.getOrElseUpdate(name, UUID.randomUUID.toString)

}
