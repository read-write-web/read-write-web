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

package auth

import org.w3.readwriteweb.WebCache
import java.io.File
import unfiltered.Cycle
import unfiltered.request.{QueryString, Path}
import xml.{Elem, XML}
import unfiltered.response.{Html, Ok}
import org.w3.readwriteweb.auth.NoX509
import org.fusesource.scalate.scuery.Transformer

/**
 * @author Henry Story
 *
 */

trait WebIDSrvc[Req,Res] {
  implicit def wc: WebCache
  implicit def manif: Manifest[Req]

  val fileDir: File = new File(this.getClass.getResource("/template/").toURI)

  lazy val webidSrvc: Elem = XML.loadFile(new File(fileDir, "WebIdService.main.xhtml"))
  lazy val noX509: Elem = XML.loadFile(new File(fileDir, "NoWebId.xhtml"))

  def intent : Cycle.Intent[Req,Res] = {
    case req @ Path("/srv/idp")  => req match {
      case QueryString(query) => Ok ~> Html( new ServiceFiller().apply(webidSrvc) )
      case _ => Ok ~> Html (new NoX509().apply(noX509))
    }

  }
}

class ServiceFiller extends Transformer {

}