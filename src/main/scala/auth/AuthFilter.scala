/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms are permitted
 * provided that the above copyright notice and this paragraph are
 * duplicated in all such forms and that any documentation,
 * advertising materials, and other materials related to such
 * distribution and use acknowledge that the software was developed
 * by Henry Story.  The name of bblfish.net may not be used to endorse
 * or promote products derived
 * from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.w3.readwriteweb.auth

import java.security.cert.X509Certificate
import javax.servlet._
import org.slf4j.LoggerFactory
import org.w3.readwriteweb._

import java.util.{LinkedList, Date}
import java.security.interfaces.RSAPublicKey
import java.security.{Principal, PublicKey}
import java.net.URL
import java.math.BigInteger
import com.hp.hpl.jena.rdf.model.RDFNode
import collection.JavaConversions._
import javax.security.auth.{Subject, Refreshable}
import com.hp.hpl.jena.query._
import org.apache.shiro.authc.AuthenticationToken

/**
 * @author Henry Story from http://bblfish.net/
 * @created: 09/10/2011
 */

case class WebIdPrincipal(webid: String) extends Principal {
  def getName = webid
  override def equals(that: Any) = that match {
    case other: WebIdPrincipal => other.webid == webid
    case _ => false
  }
}

case class Anonymous() extends Principal {
  def getName = "anonymous"
  override def equals(that: Any) =  that match {
      case other: WebIdPrincipal => other eq this 
      case _ => false
    } //anonymous principals are equal only when they are identical. is this wise?
      //well we don't know when two anonymous people are the same or different.
}

class AuthFilter(implicit webCache: WebCache) extends Filter {
  def init(filterConfig: FilterConfig) {}

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val certChain = request.getAttribute("javax.servlet.request.X509Certificate") match {
      case certs: Array[X509Certificate] => certs.toList
      case _ => Nil
    }
    val subject = new Subject()
    if (certChain.size == 0) {
      System.err.println("No certificate found!")
      subject.getPrincipals.add(Anonymous())
    } else {
      val x509c = new X509Claim(certChain(0))
      subject.getPublicCredentials.add(x509c)


      val verified = for (
        claim <- x509c.webidclaims;
        if (claim.verified)
      ) yield claim.principal

      subject.getPrincipals.addAll(verified)
      System.err.println("Found "+verified.size+" principals: "+verified)
    }

    chain.doFilter(request, response)
  }

  def destroy() {}
}

object X509Claim {
  final val logger = LoggerFactory.getLogger(classOf[X509Claim])

  /**
   * Extracts the URIs in the subject alternative name extension of an X.509
   * certificate
   *
   * @param cert X.509 certificate from which to extract the URIs.
   * @return Iterator of URIs as strings found in the subjectAltName extension.
   */
	def getClaimedWebIds(cert: X509Certificate): Iterator[String] =
    if (cert == null)  Iterator.empty;
    else cert.getSubjectAlternativeNames() match {
      case coll if (coll != null) => {
        for (sanPair <- coll
             if (sanPair.get(0) == 6)
        ) yield sanPair(1).asInstanceOf[String]
      }.iterator
      case _ => Iterator.empty
    }



}


/**
 * An X509 Claim maintains information about the proofs associated with claims
 * found in an X509 Certificate. It is the type of object that can be passed
 * into the public credentials part of a Subject node
 *
 * todo: think of what this would look like for a chain of certificates
 *
 * @author bblfish
 * @created: 30/03/2011
 */
class X509Claim(val cert: X509Certificate)(implicit webCache: WebCache) extends Refreshable with AuthenticationToken {

  import X509Claim._
  val claimReceivedDate = new Date();
  lazy val tooLate = claimReceivedDate.after(cert.getNotAfter())
  lazy val tooEarly = claimReceivedDate.before(cert.getNotBefore())

  /* a list of unverified principals */
  lazy val webidclaims = getClaimedWebIds(cert).map {
    webid =>new WebIDClaim(webid, cert.getPublicKey)
  }.toSet


  //note could also implement Destroyable
  //
  //http://download.oracle.com/javase/6/docs/technotes/guides/security/jaas/JAASRefGuide.html#Credentials
  //
  //if updating validity periods can also take into account the WebID reference, then it is possible
  //that a refresh could have as consequence to do a fetch on the WebID profile
  //note: one could also take the validity period to be dependent on the validity of the profile representation
  //in which case updating the validity period would make more sense.

  override
  def refresh() {
  }

  /* The certificate is currently within the valid time zone */
  override
  def isCurrent(): Boolean = !(tooLate||tooEarly)

  lazy val error = {}

  def canEqual(other: Any) = other.isInstanceOf[X509Claim]

  override
  def equals(other: Any): Boolean =
    other match {
      case that: X509Claim => (that eq this) || (that.canEqual(this) && cert == that.cert)
      case _ => false
    }

  override
  lazy val hashCode: Int = 41 * (41 +
    (if (cert != null) cert.hashCode else 0))

  //for Shiro, we'll think of the cert as a principal as well as a credential
  def getPrincipal = cert

  def getCredentials = cert
}

object WebIDClaim {
     final val cert: String = "http://www.w3.org/ns/auth/cert#"
     final val xsd: String = "http://www.w3.org/2001/XMLSchema#"

    val selectQuery = QueryFactory.create("""
 		  PREFIX cert: <http://www.w3.org/ns/auth/cert#>
 		  PREFIX rsa: <http://www.w3.org/ns/auth/rsa#>
 		  SELECT ?key ?m ?e ?mod ?exp
 		  WHERE {
 		   ?key cert:identity ?webid ;
 		      rsa:modulus ?m ;
 		      rsa:public_exponent ?e .
 
 		    OPTIONAL { ?m cert:hex ?mod . }
 		    OPTIONAL { ?e cert:decimal ?exp . }
 		  }""")

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
 * An X509 Claim maintains information about the proofs associated with claims
 * found in an X509 Certificate. It is the type of object that can be passed
 * into the public credentials part of a Subject node
 *
 * todo: think of what this would look like for a chain of certificates
 *
 * @author bblfish
 * @created 30/03/2011
 */
class WebIDClaim(val webId: String, val key: PublicKey)(implicit cache: WebCache) extends AuthenticationToken {

	val errors = new LinkedList[java.lang.Throwable]()

	lazy val principal = new WebIdPrincipal(webId)
	lazy val tests: List[Verification] = verify()  //I need to try to keep more verification state


	/**
	 * verify this claim
	 * @param authSrvc: the authentication service contains information about where to get graphs
	 */
	//todo: make this asynchronous
	lazy val verified: Boolean =  tests.exists(v => v.isInstanceOf[Verified])

  private def verify(): List[Verification] = {
    import util.wrapValidation
    import collection.JavaConversions._
    import WebIDClaim._
    if (!webId.startsWith("http:") && !webId.startsWith("https:")) {
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

  def getPrincipal = webId

  def getCredentials = key
}


class Verification(msg: String)
class Verified(msg: String) extends Verification(msg)
class Unverified(msg: String) extends Verification(msg)

class TestFailure(msg: String) extends Verification(msg)
class ProfileError(msg: String,  t: Throwable ) extends TestFailure(msg)
class KeyProblem(msg: String) extends TestFailure(msg)

object unsupportedProtocol extends TestFailure("WebID protocol not supported")
object noMatchingKey extends TestFailure("No keys in profile matches key in cert")
object keyDoesNotMatch extends TestFailure("Key does not match")

object verifiedWebID extends Verified("WebId verified")
object notstarted extends Unverified("No verification attempt started")
object failed extends Unverified("Tests failed")
object certificateKeyTypeNotSupported extends TestFailure("The certificate key type is not supported. We only support RSA")