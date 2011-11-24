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

import java.security.PublicKey
import org.w3.readwriteweb.WebCache
import java.security.interfaces.RSAPublicKey
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryExecution, QuerySolutionMap, QueryFactory}
import java.net.URL
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype

/**
 * @author hjs
 * @created: 13/10/2011
 */

object WebIDClaim {
    final val cert: String = "http://www.w3.org/ns/auth/cert#"

    val askQuery = QueryFactory.create("""
      PREFIX : <http://www.w3.org/ns/auth/cert#>
      ASK {
          ?webid :key [ :modulus ?m ;
                        :exponent ?e ].
      }""")

     def hex(bytes: Array[Byte]): String = bytes.map("%02X" format _).mkString.stripPrefix("00")

}

/**
 * A claim that the user identified by the WebId controls the public key
 *
 * @author bblfish
 * @created 30/03/2011
 */
class WebIDClaim(val webId: String, val key: PublicKey) {

	lazy val principal = new WebIdPrincipal(webId)

	var tests: List[Verification] = List()

  /** the tests have been done and are still valid - the idea is perhaps after a time tests would
   * have to be done again? Eg: the claim is cached and re-used after a while */
  private var valid = false

  def verified(implicit cache: WebCache): Boolean = {
    if (!valid) tests = verify(cache)
    tests.contains(verifiedWebID)
  }
  
  private def verify(implicit cache: WebCache): List[Verification] = {
    import org.w3.readwriteweb.util.wrapValidation

    import WebIDClaim._
    try {
      return if (!webId.startsWith("http:") && !webId.startsWith("https:")) {
        //todo: ftp, and ftps should also be doable, though content negotiations is then lacking
        unsupportedProtocol::Nil
      } else if (!key.isInstanceOf[RSAPublicKey]) {
        certificateKeyTypeNotSupported::Nil
      } else {
        val res = for {
          model <- cache.resource(new URL(webId)).get() failMap {
            t => new ProfileError("error fetching profile", t)
          }
        } yield {
          val rsakey = key.asInstanceOf[RSAPublicKey]
          val initialBinding = new QuerySolutionMap();
          initialBinding.add("webid",model.createResource(webId))
          initialBinding.add("m",model.createTypedLiteral( hex(rsakey.getModulus.toByteArray), XSDDatatype.XSDhexBinary))
          initialBinding.add("e",model.createTypedLiteral( rsakey.getPublicExponent.toString, XSDDatatype.XSDinteger ))
          val qe: QueryExecution = QueryExecutionFactory.create(WebIDClaim.askQuery, model,initialBinding)
          try {
            if (qe.execAsk()) verifiedWebID
            else noMatchingKey
          } finally {
            qe.close()
          }
        }
        res.either match {
          case Right(tests) => tests::Nil
          case Left(profileErr) => profileErr::Nil
        }
      }
    } finally {
      valid = true
    }


  }

	def canEqual(other: Any) = other.isInstanceOf[WebIDClaim]

	override
	def equals(other: Any): Boolean =
		other match {
			case that: WebIDClaim => (that eq this) || (that.canEqual(this) && webId == that.webId && key == that.key)
			case _ => false
		}

	override
	lazy val hashCode: Int = 41 * (
		41 * (
			41 + (if (webId != null) webId.hashCode else 0)
			) + (if (key != null) key.hashCode else 0)
		)

}

class ProfileError(msg: String,  t: Throwable) extends Verification(profileOkTst,failed,msg, Some(t))
class KeyProblem(msg: String) extends Verification(profileWellFormedKeyTst,failed,msg)

object keyDoesNotMatch extends Verification(null,null,null) //this test will be forgotten

object verifiedWebID extends Verification(webidClaimTst, passed, "WebId verified")
object noMatchingKey extends Verification(webidClaimTst, failed, "No keys in profile matches key in cert")
object unsupportedProtocol extends Verification(webidClaimTst,untested,"WebID protocol not supported" )

object certificateKeyTypeNotSupported extends Verification(pubkeyTypeTst,failed,"The certificate key type is not supported. We only support RSA")


