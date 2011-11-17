/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * under the MIT licence defined at
 *    http://www.opensource.org/licenses/mit-license.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.w3.readwriteweb.ui

import java.io.File
import xml.XML
import unfiltered.response.{Ok, Html}
import unfiltered.request.Path
import unfiltered.Cycle
import org.fusesource.scalate.scuery.Transformer
import org.fusesource.scalate.{Binding, TemplateEngine}
import unfiltered.scalate.Scalate

/**
 *
 * @author Henry Story
 */

trait WebIDAuthnReport[A,B] {

  val fileDir: File = new File(this.getClass.getResource("/tmp/").toURI)
  val templateDirs = List(fileDir)
  implicit val engine = new TemplateEngine(templateDirs)
  implicit val bindings: List[Binding] = List(Binding(name = "title", className = "String"))
  implicit val additionalAttributes = List(("title", "My First Title"))


  def intent : Cycle.Intent[A,B] = {
    case req @ Path("/test/WebIdAuth") =>  Ok ~> Html(transformer.apply(XML.loadFile(new File(fileDir,"hello.html"))))
    case req @ Path("/test/WebIdAuth2") => Ok ~> Scalate(req, "hello.ssp")
  }

}

object transformer extends Transformer {
  $(".title").contents = "The Real Title"

}
