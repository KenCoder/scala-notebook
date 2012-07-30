/*
Copyright (c) 2012 Kenneth Vogel
Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
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
