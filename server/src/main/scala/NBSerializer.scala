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
import net.liftweb.json.Serialization

/**
 * Author: Ken
 * The core object model. Fields are pythonic naming convention to match javascript
 */


object NBSerializer {
  trait Output
  case class ScalaOutput(prompt_number: Int, text: String) extends Output
  case class ScalaError(prompt_number: Int, text: String) extends Output

  trait Cell
  case class CodeCell(input: String, language: String, collapsed: Boolean,prompt_number:Option[Int], outputs: List[Output]) extends Cell
  case class MarkdownCell(source: String) extends Cell
  case class Metadata(name: String)
  case class Worksheet(cells: List[Cell])
  case class Notebook(metadata: Metadata, worksheets: List[Worksheet], nbformat: Option[Int]) {
    def name = metadata.name
  }

  // Short type hints for inner classes of this class
  case class NBTypeHints(hints: List[Class[_]]) extends TypeHints {
    def hintFor(clazz: Class[_]) = clazz.getName.substring(clazz.getName.lastIndexOf("$")+1)
    def classFor(hint: String) = hints find (hintFor(_) == hint)
  }


  implicit val formats = Serialization.formats(NBTypeHints(List(classOf[CodeCell], classOf[MarkdownCell], classOf[ScalaOutput], classOf[ScalaError])))
  val translations = List( ("cell_type", "code", "CodeCell"), ("cell_type", "markdown", "MarkdownCell"), ("output_type", "pyout", "ScalaOutput"))

  def write(nb: Notebook): String = {
    val json = Extraction.decompose(nb)

    val mapped = json transform {
      case JField("jsonClass", JString(x)) =>
        val (typ, cat, _) =
          (translations filter { _._3 == x }).head
        JField(typ, JString(cat))
      }
    compact(render(mapped))
  }

  def read(s: String): Notebook = {
    val json = parse(s)
    val mapped = json transform {
      case JField(typ, JString(cat)) if (translations exists { _._1 == typ}) =>
        val (_, _, clazz) = (translations filter { x => x._1 == typ && x._2 == cat }).head
        JField("jsonClass", JString(clazz))
    }
    mapped.extract[Notebook]
  }

}
//  def reads(s: String) : Notebook = {
//    val json = parse(s)
//    val res = for {
//      JObject(child) <- json
//      JField("name", JString(name)) <- child
//      JField("worksheets", JArray(sheet :: Nil)) <- child
//      JObject(ws) <- sheet
//      JField("cells", JArray(cells)) <- ws
//    } yield Notebook(name, List(Worksheet(cells map parseCell)))
//
//    res.head
//
//  }
//
//  def parseCell(lst: JValue) = {
//    lst match {
//      case JObject if (obj.values("cell_type") == "code") =>
//        val JField("collapsed", JBool(collapsed)) =
//        val JField("input", JString(input)) = child
//        Code(input, collapsed, Nil)
//    }
//    val JObject(child) = lst
//    child match {
//      case JField("cell_type", JString("code")) =>
//    }
//  }
//}
//object Notebook {
//  case class LCTypeHints(hints: List[Class[_]]) extends TypeHints {
//    def hintFor(clazz: Class[_]) = clazz.getName.substring(clazz.getName.lastIndexOf(".")+1).toLowerCase
//    def classFor(hint: String) = hints find (hintFor(_) == hint)
//  }
//
//  val cellSerializer = new TypeHints extends FieldSerializer[Cell](
//    FieldSerializer.renameTo("cell_type", "jsonClass"),
//    FieldSerializer.renameFrom("jsonClass", "cell_type"))
//  val outSerializer = FieldSerializer[Cell](
//    FieldSerializer.renameTo("output_type", "jsonClass"),
//    FieldSerializer.renameFrom("jsonClass", "output_type"))
//
//
//   val formats = Serialization.formats(LCTypeHints(List(classOf[Code], classOf[Markdown]))
//}
//
