
import com.k2sw.scalanb.NBSerializer
import com.k2sw.scalanb.NBSerializer._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.FlatSpec
import net.liftweb.json._
import net.liftweb.json.Serialization

/**
 * Author: Ken
 */

class NBSerializerTests extends FlatSpec {
  val testnb = Notebook(Metadata("ken1"), List(Worksheet(List(CodeCell("1+2", "python", false, Some(2), List(ScalaOutput(2, "3")))))), None)

  "Notebook Serializer" should "write and read back a simple notebook" in {
    val s = NBSerializer.write(testnb)
    val res = NBSerializer.read(s)
    assert(testnb === res)
  }
  it should "read data scraped from iPython " in {
    val ipData = """{"worksheets":[{"cells":[{"input":"1+2","cell_type":"code","prompt_number":1,"outputs":[{"output_type":"pyout","prompt_number":1,"text":"res0: Int = 3\n"}],"language":"python","collapsed":false},{"input":"","cell_type":"code","outputs":[],"language":"python","collapsed":true}]}],"metadata":{"name":"Untitled2"},"nbformat":3}"""
    println(pretty(render(parse(NBSerializer.write(testnb)))))
    println(pretty(render(parse(ipData))))
    val nb = NBSerializer.read(ipData)
    assert (nb.name === "Untitled2")
    nb.worksheets.head.cells.head match {
      case CodeCell("1+2", _, _, _, _) =>
      case x => throw new TestFailedException("Expected serialized notebook, got " + x, 0)
    }
  }

}
