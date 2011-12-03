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

import org.w3.readwriteweb.WebCache
import java.security.interfaces.RSAPublicKey
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryExecution, QuerySolutionMap, QueryFactory}
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import scalaz.{Scalaz, Validation}
import Scalaz._
import java.net.URL
import java.security.PublicKey
import com.hp.hpl.jena.rdf.model.Model


/**
 * @author Henry Story
 * @created: 13/10/2011
 */

/**
 * One can only construct a WebID via the WebIDClaim apply
 */
object WebIDClaim {
  final val cert: String = "http://www.w3.org/ns/auth/cert#"

  val askQuery = QueryFactory.create("""
      PREFIX : <http://www.w3.org/ns/auth/cert#>
      ASK {
          ?webid :key [ :modulus ?m ;
                        :exponent ?e ].
      }""")

  def hex(bytes: Array[Byte]): String = bytes.dropWhile(_ == 0).map("%02X" format _).mkString

}

/**
 * One has to construct a WebID using the object, that can do basic verifications
 */
class WebIDClaim(val san: String, val key: PublicKey)(implicit cache: WebCache) {

  import WebIDClaim.hex
  import XSDDatatype._

  private def rsaTest(webid: WebID, rsakey: RSAPublicKey): (Model) => Validation[WebIDVerificationFailure, WebID] = {
    model =>
      val initialBinding = new QuerySolutionMap();
      initialBinding.add("webid", model.createResource(webid.url.toString))
      initialBinding.add("m", model.createTypedLiteral(hex(rsakey.getModulus.toByteArray), XSDhexBinary))
      initialBinding.add("e", model.createTypedLiteral(rsakey.getPublicExponent.toString, XSDinteger))
      val qe: QueryExecution = QueryExecutionFactory.create(WebIDClaim.askQuery, model, initialBinding)
      try {
        if (qe.execAsk()) webid.success
        else new WebIDVerificationFailure("could not verify public key", None, this).fail
      } finally {
        qe.close()
      }
  }

  lazy val verify: Validation[WebIDClaimFailure, WebID] = key match {
      case rsakey: RSAPublicKey =>
        WebID(san).flatMap(webid=> webid.getDefiningModel.flatMap(rsaTest(webid, rsakey)) )
      case _ => new UnsupportedKeyType("We only support RSA keys at present", key).fail
    }
  }


trait Err {
  type T <: AnyRef
  val msg: String
  val cause: Option[Throwable]=None
  val subject: T
}

abstract class Fail extends Throwable with Err

abstract class WebIDClaimFailure extends Fail

class UnsupportedKeyType(val msg: String, val subject: PublicKey) extends WebIDClaimFailure { type T = PublicKey }


abstract class SANFailure extends WebIDClaimFailure { type T = String }
case class UnsupportedProtocol(val msg: String, subject: String) extends SANFailure
case class URISyntaxError(val msg: String, subject: String) extends SANFailure

//The subject could be more refined than the URL, especially in the paring error
abstract class ProfileError extends WebIDClaimFailure  { type T = URL }
case class ProfileGetError(val msg: String,  override val cause: Option[Throwable], subject: URL) extends ProfileError
case class ProfileParseError(val msg: String, override val cause: Option[Throwable], subject: URL) extends ProfileError

//it would be useful to pass the graph in
class WebIDVerificationFailure(val msg: String, val caused: Option[Throwable], val subject: WebIDClaim)
  extends WebIDClaimFailure { type T = WebIDClaim }