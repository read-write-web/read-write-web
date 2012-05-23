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

import unfiltered.Cycle
import xml.{Elem, XML}
import unfiltered.request.Path
import org.fusesource.scalate.scuery.{Transform, Transformer}
import unfiltered.scalate.Scalate
import java.text.DateFormat
import java.util.Date
import unfiltered.response.{CacheControl, Expires, Ok, Html}

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
   implicit def manif: Manifest[Req]

  val fileDir = "/template/"

  lazy val webidTst: Elem = XML.load(this.getClass.getResourceAsStream(fileDir+"WebId.xhtml"))
  lazy val noX509: Elem = XML.load(this.getClass.getResourceAsStream(fileDir+"NoWebId.xhtml"))
  
  def intent : Cycle.Intent[Req,Res] = {
    case req @ Path("/test/WebId")  => req match {
      case X509Claim(claim) => Ok ~> Html( new X509Filler(claim).apply(webidTst) ) ~> Expires("0") ~> CacheControl("no-cache")
      case _ => Ok ~> Html (new NoX509().apply(noX509)) ~> Expires("0") ~> CacheControl("no-cache")
    }
    case req @ Path("/test/WebIdAuth2") => Ok ~> Scalate(req, "hello.ssp")
  }

}

class NoX509() extends Transformer {
  $(".date").contents = DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.LONG).format(new Date)
}

class X509Filler(x509: X509Claim) extends Transformer {
  def pretty(res: Outcome) = {
    res match {
      case org.w3.readwriteweb.auth.passed => <span class="outcome_passed">passed</span>
      case org.w3.readwriteweb.auth.failed => <span class="outcome_failed">failed</span>
      case org.w3.readwriteweb.auth.untested => <span class="outcome_untested">untested</span>
    }
  }
  $(".date").contents = DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.LONG).format( x509.claimReceivedDate)
  $(".cert_test") { node =>
      val x509Assertion = new Assertion(certOk,x509);
      val x509Assertions = x509Assertion::x509Assertion.depends
      val ff = for (ast <- x509Assertions) yield {
        new Transform(node) {
          $(".tst_question").contents = ast.test.title
          $(".tst_txt").contents = ast.test.description
          $(".tst_res").contents = pretty(ast.result.outcome)
          $(".tst_res_txt").contents = ast.result.description
        }.toNodes()
      }
      ff.flatten
  }
  $(".san_number").contents = if (x509.claims.size == 0) "No" else x509.claims.size.toString
  $(".san_verified") { node => if (x509.claims.size==0) <span/> else
    new Transform(node) {
      $(".san_verified_no").contents = x509.verified.size.toString
    }.toNodes()
  }

  $(".webid_test") { node =>
    val ff = for (idclaim <- x509.claims) yield {
      val idAsrt = new Assertion(webidClaimTst, idclaim)
      new Transform(node) {
        $(".webid").contents = idclaim.san
        $(".tst_res_txt").contents = idAsrt.result.description
        $(".tst_res").contents = pretty(idAsrt.result.outcome)
        $(".webid_cause") { n2 =>
          val nn = for (a <- idAsrt.depends) yield {
            new Transform(n2) {
              $(".cause_question").contents = a.test.title
              $(".cause_txt").contents = a.test.description
              $(".cause_res").contents = a.result.outcome.name
            }.toNodes()
          }
          nn.flatten
        }
      }.toNodes()
    }
    ff.flatten
  }
  $(".certificate").contents = x509.cert.toString

}
