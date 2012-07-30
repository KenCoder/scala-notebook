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

import org.fusesource.scalate.{
  TemplateEngine, Binding, DefaultRenderContext, RenderContext}
import unfiltered.request.{Path,HttpRequest}
import unfiltered.response.{ResponseWriter}
import java.io.{File,OutputStreamWriter,PrintWriter}

object Scalate {
  /** Constructs a ResponseWriter for Scalate templates.
   *  Note that any parameter in the second, implicit set
   *  can be overriden by specifying an implicit value of the
   *  expected type in a pariticular scope. */
  def apply[A, B](request: HttpRequest[A],
                  template: String,
                  attributes:(String,Any)*)
  ( implicit
    engine: TemplateEngine = defaultEngine,
    contextBuilder: ToRenderContext = defaultRenderContext,
    bindings: List[Binding] = Nil,
    additionalAttributes: Seq[(String, Any)] = Nil
  ) = new ResponseWriter {
    def write(writer: OutputStreamWriter) {
      val printWriter = new PrintWriter(writer)
      try {
        val scalateTemplate = engine.load(template, bindings)
        val context = contextBuilder(Path(request), printWriter, engine)
        (additionalAttributes ++ attributes) foreach {
          case (k,v) => context.attributes(k) = v
        }
        engine.layout(scalateTemplate, context)
      } catch {
        case e if engine.isDevelopmentMode =>
          printWriter.println("Exception: " + e.getMessage)
          e.getStackTrace.foreach(printWriter.println)
        case e => throw e
      }
    }
  }

  /* Function to construct a RenderContext. */
  type ToRenderContext =
    (String, PrintWriter, TemplateEngine) => RenderContext

  private val defaultTemplateDirs = 
    new File("src/main/resources/templates") :: Nil
  private val defaultEngine = new TemplateEngine(defaultTemplateDirs)
  private val defaultRenderContext: ToRenderContext =
    (path, writer, engine) =>
      new DefaultRenderContext(path, engine, writer)
}
