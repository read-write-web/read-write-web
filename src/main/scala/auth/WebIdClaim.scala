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
import org.w3.readwriteweb.util.wrapValidation


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

  def apply(san: String, rsakey: RSAPublicKey)(implicit cache: WebCache): Validation[WebIDClaimErr, WebIDClaim] =
    for (id <- WebID(san) failMap {
      case e => new WebIDClaimErr("Unsupported WebID", Some(e))
    })
    yield new WebIDClaim(id,rsakey)
}

/**
 * One has to construct a WebID using the object, that can do basic verifications
 */
class WebIDClaim private (val webid: WebID, val rsakey: RSAPublicKey)(implicit cache: WebCache) {
  import WebIDClaim.hex
  import XSDDatatype._

  lazy val verify: Validation[WebIDClaimErr,WebID] =
    webid.getDefiningModel.failMap {
      case e => new WebIDClaimErr("could not fetch model", Some(e))
    }.flatMap { model =>
      val initialBinding = new QuerySolutionMap();
      initialBinding.add("webid", model.createResource(webid.url.toString))
      initialBinding.add("m", model.createTypedLiteral(hex(rsakey.getModulus.toByteArray), XSDhexBinary))
      initialBinding.add("e", model.createTypedLiteral(rsakey.getPublicExponent.toString, XSDinteger))
      val qe: QueryExecution = QueryExecutionFactory.create(WebIDClaim.askQuery, model, initialBinding)
      try {
        if (qe.execAsk()) webid.success
        else new WebIDClaimErr("could not verify public key").fail
      } finally {
        qe.close()
      }
    }
}

trait Err {
  val msg: String
  val cause: Option[Throwable]=None
}

abstract class Failure extends Throwable with Err

abstract class SANFailure extends Failure
case class UnsupportedProtocol(val msg: String) extends SANFailure
case class URISyntaxError(val msg: String) extends SANFailure

abstract class ProfileError extends Failure
case class ProfileGetError(val msg: String,  override val cause: Option[Throwable]) extends ProfileError
case class ProfileParseError(val msg: String, override val cause: Option[Throwable]) extends ProfileError

//it would be useful to pass the graph in
class WebIDClaimErr(val msg: String, override val cause: Option[Throwable]=None) extends Failure
