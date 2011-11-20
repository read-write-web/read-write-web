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

import java.math.BigInteger
import java.security.PublicKey
import org.w3.readwriteweb.WebCache
import java.security.interfaces.RSAPublicKey
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryExecution, QuerySolutionMap, QueryFactory}
import com.hp.hpl.jena.rdf.model.RDFNode
import java.net.URL

/**
 * @author hjs
 * @created: 13/10/2011
 */

object WebIDClaim {
     final val cert: String = "http://www.w3.org/ns/auth/cert#"
     final val xsd: String = "http://www.w3.org/2001/XMLSchema#"

    val selectQuery = QueryFactory.create("""
      PREFIX cert: <http://www.w3.org/ns/auth/cert#>
      PREFIX rsa: <http://www.w3.org/ns/auth/rsa#>
      SELECT ?m ?e ?mod ?exp
      WHERE {
         {
           ?key  cert:identity ?webid .
         } UNION {
           ?webid cert:key ?key .
         }
          ?key rsa:modulus ?m ;
               rsa:public_exponent ?e .

       OPTIONAL { ?m cert:hex ?mod . }
       OPTIONAL { ?e cert:decimal ?exp . }
      }""") //Including OPTIONAL notation, for backward compatibility - should remove that after a while

  /**
    * Transform an RDF representation of a number into a BigInteger
    * <p/>
    * Passes a statement as two bindings and the relation between them. The
    * subject is the number. If num is already a literal number, that is
    * returned, otherwise if enough information from the relation to optstr
    * exists, that is used.
    *
    * @param num the number node
    * @param optRel name of the relation to the literal
    * @param optstr the literal representation if it exists
    * @return the big integer that num represents, or null if undetermined
    */
   def toInteger(num: RDFNode, optRel: String, optstr: RDFNode): Option[BigInteger] =
       if (null == num) None
       else if (num.isLiteral) {
         val lit = num.asLiteral()
         toInteger_helper(lit.getLexicalForm,lit.getDatatypeURI)
       } else if (null != optstr && optstr.isLiteral) {
           toInteger_helper(optstr.asLiteral().getLexicalForm,optRel)
       } else None


    private def intValueOfHexString(s: String): BigInteger = {
      val strval = cleanHex(s);
      new BigInteger(strval, 16);
    }


    /**
     * This takes any string and returns in order only those characters that are
     * part of a hex string
     *
     * @param strval
     *            any string
     * @return a pure hex string
     */

    private def cleanHex(strval: String) = {
      def legal(c: Char) = {
        //in order of appearance probability
        ((c >= '0') && (c <= '9')) ||
          ((c >= 'A') && (c <= 'F')) ||
          ((c >= 'a') && (c <= 'f'))
      }
      (for (c <- strval; if legal(c)) yield c)
    }


   /**
    * This transforms a literal into a number if possible ie, it returns the
    * BigInteger of "ddd"^^type
    *
    * @param num the string representation of the number
    * @param tpe the type of the string representation
    * @return the number
    */
   protected def toInteger_helper(num: String, tpe: String): Option[BigInteger] =
     try {
       if (tpe.equals(cert + "decimal") || tpe.equals(cert + "int")
         || tpe.equals(xsd + "integer") || tpe.equals(xsd + "int")
         || tpe.equals(xsd + "nonNegativeInteger")) {
         // cert:decimal is deprecated
         Some(new BigInteger(num.trim(), 10));
       } else if (tpe.equals(cert + "hex")) {
         Some(intValueOfHexString(num));
       } else {
         // it could be some other encoding - one should really write a
         // special literal transformation class
         None;
       }
     } catch {
       case e: NumberFormatException => None
     }


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

  private var valid = false

  def verified(implicit cache: WebCache): Boolean = {
    if (!valid) tests = verify(cache)
    tests.contains(verifiedWebID)
  }
  
  private def verify(implicit cache: WebCache): List[Verification] = {
    import org.w3.readwriteweb.util.wrapValidation

    import collection.JavaConversions._
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
          val initialBinding = new QuerySolutionMap();
          initialBinding.add("webid",model.createResource(webId))
          val qe: QueryExecution = QueryExecutionFactory.create(WebIDClaim.selectQuery, model,initialBinding)
          try {
            qe.execSelect().map( qs => {
              val modulus = toInteger(qs.get("m"), cert + "hex", qs.get("mod"))
              val exponent = toInteger(qs.get("e"), cert + "decimal", qs.get("exp"))

              (modulus, exponent) match {
                case (Some(mod), Some(exp)) => {
                  val rsakey = key.asInstanceOf[RSAPublicKey]
                  if (rsakey.getPublicExponent == exp && rsakey.getModulus == mod) verifiedWebID
                  else keyDoesNotMatch
                }
                case _ => new KeyProblem("profile contains key that cannot be analysed:" +
                  qs.varNames().map(nm => nm + "=" + qs.get(nm).toString) + "; ")
              }
            }).toList
            //it would be nice if we could keep a lot more state of what was verified and how
            //will do that when implementing tests, so that these tests can then be used directly as much as possible
          } finally {
            qe.close()
          }
        }
        res.either match {
          case Right(tests) => tests
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


