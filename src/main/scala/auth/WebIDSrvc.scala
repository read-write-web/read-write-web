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

import unfiltered.Cycle
import java.net.{URLEncoder, URL}
import java.util.Calendar
import org.apache.commons.codec.binary.Base64
import java.text.SimpleDateFormat
import org.w3.readwriteweb.util.trySome
import unfiltered.request.Params.ParamMapper
import com.hp.hpl.jena.rdf.model.ModelFactory
import sommer.{CertAgent, Extractors}
import org.w3.readwriteweb.util._
import org.w3.readwriteweb.auth._
import unfiltered.request._
import org.fusesource.scalate.scuery.{Transform, Transformer}
import org.w3.readwriteweb.netty.ReadWriteWebNetty.StaticFiles
import java.lang.String
import xml._
import unfiltered.response._
import java.security.interfaces.RSAPublicKey

object WebIDSrvc {
  val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

  val noImage = "idp/profile_anonymous.png" //"http://eagereyes.org/media/2010/empty-frame.jpg"

}

/**
 * @author Henry Story
 *
 */
trait WebIDSrvc[Req,Res] {
  implicit def manif: Manifest[Req]
  val signer: X509CertSigner

  import WebIDSrvc._



  def sign(urlStr: String): URL = {
    val timeStampedUrlStr = urlStr + "ts=" + URLEncoder.encode(dateFormat.format(Calendar.getInstance.getTime), "UTF-8")
    val signedUri = timeStampedUrlStr +
      "&sig=" + URLEncoder.encode(new String(Base64.encodeBase64URLSafeString(signer.sign(timeStampedUrlStr))), "UTF-8")
    return new URL(signedUri)
  }


  val fileDir ="/template/webidp/"

  /**
   * using three different templates uses up more memory for the moment, and could be more maintenance work
   * if all three templates require similar changes, but it makes it easier to visualise the result without
   * needing a web server. 
   */
  lazy val errorPg: Elem = XML.load(this.getClass.getResourceAsStream(fileDir+ "WebIdService.badcert.html"))
  lazy val authenticatedPg: Elem = XML.load(this.getClass.getResourceAsStream(fileDir+ "WebIdService.auth.html"))
  lazy val aboutPg: Elem = XML.load(this.getClass.getResourceAsStream(fileDir+ "WebIdService.about.html"))
  lazy val profilePg: Elem = XML.load(this.getClass.getResourceAsStream(fileDir+ "WebIdService.entry.html"))

  def intent : Cycle.Intent[Req,Res] = {
    case req @ Path(Seg("srv" :: "idp" :: next))  => { //easy partial function entry match
      if (next!=Nil && next.size==1) srvStaticFiles(next.head)
      else req match {
        case Params(RelyingParty(rp)) => req match {
          case Params(DoIt(_)) & XClaim(claim: XClaim) => {
            val answer = if (claim == NoClaim) "error=nocert&"
            else if (claim.claims.size == 0) "error=noWebID&"
            else if (claim.verified.size == 0) "error=noVerifiedWebID&" +
              claim.claims.map(claim => claim.verify.failMap(e => "cause="+e.getMessage)).mkString("&")+"&"
            else claim.verified.slice(0, 3).foldRight("") {
              (wid, str) => "webid=" + URLEncoder.encode(wid.url.toExternalForm, "UTF-8") + "&"
            }
            val signedAnswer = sign(rp.toExternalForm + "?" + answer).toExternalForm
            Redirect(signedAnswer)
          }
          //GET=>The user just arrived on the page. We recuperated the X509 claim in case he has authenticated already
          case GET(_) & XClaim(claim: XClaim) => {
            val pg = claim match {
              case NoClaim => profilePg
              case claim: X509Claim => if (claim.verified.size > 0) authenticatedPg else errorPg
            }
            Ok ~> Html5(new ServiceTrans(rp, claim).apply(pg))
          }
          //POST=> we authenticate the user because he has agreed to be authenticated on the page, which we know if the
          // request is a POST
          case POST(_) & X509Claim(claim: X509Claim) => {
            //repetition because of intellij scala 0.5.273 bug
            val pg = if (claim.verified.size > 0) authenticatedPg else errorPg
            Ok ~> Html5(new ServiceTrans(rp, claim).apply(pg))
          }
          case _ => Ok ~> Html5(new ServiceTrans(rp, NoClaim).apply(errorPg))
        }
        case _ => Ok ~> Html5(aboutTransform.apply(aboutPg))
      }
    }

  }


  object TransUtils {
    //taken from http://stackoverflow.com/questions/2569580/how-to-change-attribute-on-scala-xml-element
    implicit def addGoodCopyToAttribute(attr: Attribute) = new {
      def goodcopy(key: String = attr.key, value: Any = attr.value): Attribute =
        Attribute(attr.pre, key, Text(value.toString), attr.next)
    }

    implicit def iterableToMetaData(items: Iterable[MetaData]): MetaData = {
      items match {
        case Nil => Null
        case head :: tail => head.copy(next=iterableToMetaData(tail))
      }
    }
  }

  object srvStaticFiles extends StaticFiles {
    override def toLocal(file: String) = "/template/webidp/idp/"+file
  }
  
  object aboutTransform extends Transformer {
    val key = signer.signingCert.getPublicKey.asInstanceOf[RSAPublicKey]
    $(".modulus").contents = key.getModulus.toString(16)
    $(".exponent").contents = key.getPublicExponent.toString
  }

  class ServiceTrans(relyingParty: URL, claim: XClaim) extends Transformer {
    $(".webidform") { node =>
      val elem = node.asInstanceOf[scala.xml.Elem]
      import TransUtils._
      val newelem = elem.copy(attributes=for(attr <- elem.attributes) yield attr match {
         case attr@Attribute("action", _, _) => attr.goodcopy(value="/srv/idp?rs="+relyingParty.toExternalForm)
         case other => other
      })
      new Transform(newelem) {
        $(".authenticated") {  node =>
          claim match {
            case NoClaim => <span/>
            case _ => new Transform(node) {
              val union = claim.verified.flatMap(_.getDefiningModel.toOption).fold(ModelFactory.createDefaultModel()) {
                (m1, m2) => m1.add(m2)
              }
              //this works because we have verified before
              val person = if (union.size() > 0) Extractors.namedPerson(union, claim.verified.head.url)
              else new CertAgent(claim.cert.getSubjectDN.getName)
              $(".user_name").contents = person.name
              $(".mugshot").attribute("src").value = person.depictions.collectFirst {
                case o => o.toString
              }.getOrElse(noImage)
            }.toNodes()
          }
        }
        $(".error").contents = {
          if (claim==NoClaim) "We received no Certificate"
          else if (claim.claims.size==0) "Certificate contained no WebID"
          else if (claim.verified.size==0) "Could not verify any of the WebIDs"
          else "Some error occured"
        }
        $(".response") { nodeRes =>
          val elemRes = nodeRes.asInstanceOf[scala.xml.Elem]
          val newElem = elemRes.copy(attributes=for(attr <- elemRes.attributes) yield attr match {
            case attr@Attribute("href", _, _) =>
              attr.goodcopy(value= "/srv/idp?rs="+URLEncoder.encode(relyingParty.toExternalForm,"UTF-8") + "&doit=true" )
            case other => other
          })
          new Transform(newElem) {
            $(".sitename").contents = relyingParty.getHost
          }.toNodes()
        }

      }.toNodes()
    }
  }
}

case class Html5(nodes: scala.xml.NodeSeq) extends ComposeResponse(HtmlContent ~> {
  val w = new java.io.StringWriter()
  val html = nodes.head match {
    case <html>{_*}</html> => nodes.head
    case _ => <html>{nodes}</html>
  }
  xml.XML.write( w, html, "UTF-8", xmlDecl = false, doctype =
    xml.dtd.DocType( "html", xml.dtd.SystemID( "about:legacy-compat" ), Nil ))
  ResponseString(w.toString)
})


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
object DoIt extends Params.Extract("doit", Params.first)
