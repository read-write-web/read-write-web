/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * under the MIT licence defined
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

package org.w3.readwriteweb.auth

import java.io.File
import unfiltered.response.{Ok, Html}
import unfiltered.Cycle
import org.fusesource.scalate.{Binding, TemplateEngine}
import xml.{Elem, XML}
import unfiltered.request.Path
import org.fusesource.scalate.scuery.{Transform, Transformer}
import org.w3.readwriteweb.WebCache
import unfiltered.scalate.Scalate
import java.text.DateFormat
import java.util.Date
import scalaz.Validation

/**
 * This plan just described the X509 WebID authentication information.
 * It works independently of the underlying Cycle.Intent implementations of Request and Response,
 * so it can work with servlet filters just as well as with netty.
 *
 * This is a simple version. A future version will show EARL output, and so be useful for debugging the endpoint.
 *
 * @author hjs
 * @created: 13/10/2011
 */

trait X509view[Req,Res]  {
   implicit def wc: WebCache
   implicit def manif: Manifest[Req]

  val fileDir: File = new File(this.getClass.getResource("/template/").toURI)
  val templateDirs = List(fileDir)
  implicit val engine = new TemplateEngine(templateDirs)
  implicit val bindings: List[Binding] = List(Binding(name = "title", className = "String"))
  implicit val additionalAttributes = List(("title", "My First Title"))

  lazy val webidTst: Elem = XML.loadFile(new File(fileDir, "WebId.xhtml"))
  lazy val noX509: Elem = XML.loadFile(new File(fileDir, "NoWebId.xhtml"))
  
  def intent : Cycle.Intent[Req,Res] = {
    case req @ Path("/test/auth/webid")  => req match {
      case X509Claim(claim) => Ok ~> Html( new X509Filler(claim).apply(webidTst) )
      case _ => Ok ~> Html (new NoX509().apply(noX509))
    }
    case req @ Path("/test/WebIdAuth2") => Ok ~> Scalate(req, "hello.ssp")
  }

}

class NoX509() extends Transformer {
  $(".date").contents = DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.LONG).format(new Date)
}

class X509Filler(x509: X509Claim)(implicit cache: WebCache) extends Transformer {
  $(".date").contents = DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.LONG).format( x509.claimReceivedDate)
  $(".cert_test") { node =>
      val x509Tests = certOk.test(x509);
      val ff = for (tst <- x509Tests) yield {
        new Transform(node) {
          $(".tst_question").contents = tst.of.title
          $(".tst_txt").contents = tst.of.description
          $(".tst_res").contents = tst.result.name
          $(".tst_res_txt").contents = tst.msg
        }.toNodes()
      }
      ff.flatten
  }
//  $(".webid_tests") { node =>
//    val ff = for (idclaim <- x509.webidclaims) yield {
//      new Transform(node) {
//        $(".webid").contents = "Testing webid " +idclaim.webid
//        $(".webid_test") { n2 =>
//          val validation: Validation[WebIDClaimErr, WebID] = idclaim.verify
//          val nn = for (tst <-idclaim.tests) yield {
//            new Transform(n2) {
//              $(".tst_question").contents = tst.of.title
//              $(".tst_txt").contents = tst.of.description
//              $(".tst_res").contents = tst.result.name
//              $(".tst_res_txt").contents = tst.msg
//            }.toNodes()
//          }
//          nn.flatten
//        }
//      }.toNodes()
//    }
//    ff.flatten
//  }
//  $(".certificate").contents = x509.cert.toString

}
