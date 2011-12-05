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

import java.io.File
import unfiltered.Cycle
import xml.{Elem, XML}
import unfiltered.response.{Html, Ok}
import org.fusesource.scalate.scuery.Transformer
import unfiltered.request.{Params, Path}
import unfiltered.request.&
import java.net.{URLEncoder, URL}
import java.util.Calendar
import org.apache.commons.codec.binary.Base64
import org.w3.readwriteweb.auth.{WebID, X509CertSigner, X509Claim}
import java.text.SimpleDateFormat
import org.w3.readwriteweb.util.trySome
import unfiltered.request.Params.ParamMapper
import com.hp.hpl.jena.sparql.vocabulary.FOAF
import com.hp.hpl.jena.rdf.model.{RDFNode, ModelFactory}

/**
 * @author Henry Story
 *
 */
trait WebIDSrvc[Req,Res] {
  implicit def manif: Manifest[Req]
  val signer: X509CertSigner


  private val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    /**
     * @param webid  a list of webIds identifying the user (only the first few will be  used)
     * @param replyTo the service that the response is sent to
     * @return the URL of the response with the webid, timestamp appended and signed
     */
  private def createSignedResponse(webid: Seq[WebID], replyTo: URL): URL = {
      var uri = replyTo.toExternalForm+"?"+ webid.slice(0,3).foldRight("") {
        (wid,str) => "webid="+  URLEncoder.encode(wid.url.toExternalForm, "UTF-8")+"&"
      }
      uri = uri + "ts=" + URLEncoder.encode(dateFormat.format(Calendar.getInstance.getTime), "UTF-8")
      val signedUri =	uri +"&sig=" + URLEncoder.encode(new String(Base64.encodeBase64URLSafeString(signer.sign(uri))), "UTF-8")
      return new URL(signedUri)
  }

  val fileDir: File = new File(this.getClass.getResource("/template/").toURI)

  lazy val webidSrvcPg: Elem = XML.loadFile(new File(fileDir, "WebIdService.login.xhtml"))
  lazy val aboutPg: Elem = XML.loadFile(new File(fileDir, "WebIdService.about.xhtml"))

  def intent : Cycle.Intent[Req,Res] = {
    case req @ Path(path) if path.startsWith("/srv/idp")  => req match {
      case Params(RelyingParty(rp)) & X509Claim(claim) => Ok ~> Html( new ServiceTrans(rp,claim).apply(webidSrvcPg) )
      case _ => Ok ~> Html(aboutTransform(aboutPg))
    }

  }
  
  object aboutTransform extends Transformer


  class ServiceTrans(relyingParty: URL, claim: X509Claim) extends Transformer {
    val union = claim.verified.flatMap(_.getDefiningModel.toOption).fold(ModelFactory.createDefaultModel()){
      (m1,m2)=>m1.add(m2)
    }
    $(".user_name").contents =
      if (claim.verified.size==0)
          claim.cert.getSubjectDN.getName //we have something, we can't rely on it, but we can use it
      else {
        val names = union.listObjectsOfProperty(union.createResource(claim.verified.head.url.toExternalForm),FOAF.name)
        if (!names.hasNext) "nonname"
        else {
          val node: RDFNode = names.next()
          if (node.isLiteral) node.asLiteral().getLexicalForm
          else "anonymous"
        }
      }

    $(".mugshot").attribute("src").value =
      if (claim.verified.size==0)
         "http://www.yourbdnews.com/wp-content/uploads/2011/08/anonymous.jpg"
      else {
        val names = union.listObjectsOfProperty(union.createResource(claim.verified.head.url.toExternalForm),FOAF.depiction)
        if (!names.hasNext) "http://www.yourbdnews.com/wp-content/uploads/2011/08/anonymous.jpg"
        else {
         names.next.toString
        }
      }

    $(".rp_name").contents = relyingParty.getHost
    $(".rp_url").attribute("href").value = createSignedResponse(claim.verified,relyingParty).toExternalForm
  }

}

/**
 * similar to Extract superclass, but is useful when one has a number of attributes that
 * have the same meaning. This can arise if one has legacy URLS or if one has code that
 * has human readable and shorter ones
 */
import unfiltered.request.Params.Map
class ExtractN[E,T](f: Map => Option[T]) extends Params.Extract[E,T](f) {
  def this(names: Seq[String], f: Seq[String] => Option[T]) =
     this({ params: Map => f(names.flatMap(s => params(s))) })
}

object urlMap extends ParamMapper(_.flatMap(u=>trySome{new URL(u)}).headOption)
object RelyingParty extends ExtractN(List("rs","authreqissuer"), urlMap )


