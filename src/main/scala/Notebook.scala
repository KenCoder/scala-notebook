package com.k2sw.scalanb

import net.liftweb.json.TypeHints

/**
 * Author: Ken
 * The core object model. Fields are pythonic naming convention to match javascript
 */

trait Output
case class ScalaOutput(prompt_number: Int, text: String) extends Output
case class ScalaError(prompt_number: Int, text: String) extends Output

trait Cell
case class CodeCell(input: List[String], collapsed: Boolean, outputs: List[Output])
case class MarkdownCell(source: String)

class Notebook(val id: String, val name: String) extends Serializable {
  val cells = collection.mutable.IndexedSeq[String]()
}

abstract class NBTypeHints extends TypeHints {
  val cells: (String, Map[String, Class[_]]) = "cell_type" -> Map("code" -> classOf[CodeCell], "markdown" -> classOf[MarkdownCell])
  val outputs: (String, Map[String, Class[_]]) = "output_type" -> Map("pyout" -> classOf[ScalaOutput])
  val groups = Seq(cells, outputs)
  def hintFor(clazz: Class[_]) = clazz.getName.substring(clazz.getName.lastIndexOf(".")+1)
//  def classFor(hint: String) = hints find (hintFor(_) == hint)
}

