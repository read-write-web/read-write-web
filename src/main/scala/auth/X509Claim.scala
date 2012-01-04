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



import org.slf4j.LoggerFactory
import java.security.cert.X509Certificate
import javax.security.auth.Refreshable
import collection.JavaConversions._
import unfiltered.request.HttpRequest
import java.security.interfaces.RSAPublicKey
import collection.immutable.List
import com.google.common.cache.{CacheLoader, CacheBuilder, Cache}
import java.util.concurrent.TimeUnit
import org.w3.readwriteweb.util.trySome
import java.util.Date

/**
 * @author hjs
 * @created: 13/10/2011
 */

object X509Claim {
  final val logger = LoggerFactory.getLogger(classOf[X509Claim])
  implicit val fetch = true //fetch the certificate if we don't have it

// this is cool because it is not in danger of running out of memory but it makes it impossible to create the claim
// with an implicit  WebCache...
  val idCache: Cache[X509Certificate, X509Claim] =
     CacheBuilder.newBuilder()
     .expireAfterWrite(30, TimeUnit.MINUTES)
     .build(new CacheLoader[X509Certificate, X509Claim] {
       def load(x509: X509Certificate) = new X509Claim(x509)
     })

  def unapply[T](r: HttpRequest[T])(implicit m: Manifest[T]): Option[X509Claim] = r match {
    case Certs(c1: X509Certificate, _*) => trySome(idCache.get(c1))
    case _ => None
  }


  /**
   * Extracts the URIs in the subject alternative name extension of an X.509
   * certificate
   *
   * @param cert X.509 certificate from which to extract the URIs.
   * @return Iterator of URIs as strings found in the subjectAltName extension.
   */
  def getClaimedWebIds(cert: X509Certificate): List[String] =
    if (cert == null) Nil
    else cert.getSubjectAlternativeNames().toList match {
      case coll if (coll != null) => {
        for {
          sanPair <- coll if (sanPair.get(0) == 6)
        } yield sanPair(1).asInstanceOf[String].trim
      }
      case _ => Nil
    }

}

object ExistingClaim {
  implicit val fetch = false //don't fetch the certificate if we don't have it -- ie, don't force the fetching of it

  def unapply[T](r: HttpRequest[T])(implicit m: Manifest[T]): Option[X509Claim] = r match {
    case Certs(c1: X509Certificate, _*) => trySome(X509Claim.idCache.get(c1))
    case _ => None
  }
  
}

object XClaim {
  //warning: it turns out that one nearly never recuperates the client certificate
  //see http://stackoverflow.com/questions/8731157/netty-https-tls-session-duration-why-is-renegotiation-needed
  implicit val fetch = false
  def unapply[T](r: HttpRequest[T])(implicit m: Manifest[T]): Option[XClaim] = r match {
    case Certs(c1: X509Certificate, _*) => trySome(X509Claim.idCache.get(c1))
    case _ => Some(NoClaim)
  }
}



/**
 * This looks like something that could be abstracted into type perhaps XClaim[X509Certificate,WebIDClaim]
 */
sealed trait XClaim {
   def cert: X509Certificate
   def valid: Boolean
   def claims: List[WebIDClaim]
   def verified: List[WebID]
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
// can't be a case class as it then creates object which clashes with defined one
class X509Claim(val cert: X509Certificate) extends Refreshable with XClaim {

  import X509Claim._
  val claimReceivedDate = new Date()
  lazy val tooLate = claimReceivedDate.after(cert.getNotAfter())
  lazy val tooEarly = claimReceivedDate.before(cert.getNotBefore())


  lazy val claims: List[WebIDClaim] = getClaimedWebIds(cert) map { webid => 
    new WebIDClaim(webid, cert.getPublicKey.asInstanceOf[RSAPublicKey]) 
  }

  lazy val verified: List[WebID] = claims.flatMap(_.verify.toOption)

  //note could also implement Destroyable
  //
  //http://download.oracle.com/javase/6/docs/technotes/guides/security/jaas/JAASRefGuide.html#Credentials
  //
  //if updating validity periods can also take into account the WebID reference, then it is possible
  //that a refresh could have as consequence to do a fetch on the WebID profile
  //note: one could also take the validity period to be dependent on the validity of the profile representation
  //in which case updating the validity period would make more sense.

  override def refresh() = ()

  /* The certificate is currently within the valid time zone */
  override def isCurrent(): Boolean = ! (tooLate || tooEarly)

  protected def canEqual(other: Any) = other.isInstanceOf[X509Claim]

  override def equals(other: Any): Boolean = other match {
    case that: X509Claim => (that eq this) || (that.canEqual(this) && cert == that.cert)
    case _ => false
  }

  override lazy val hashCode: Int =
    41 * (41 + (if (cert != null) cert.hashCode else 0))

  def valid = isCurrent()
}

/**
 *  A bit like a None for X509Claims
 */
case object NoClaim extends XClaim {
  override def cert = throw new NoSuchElementException("None.get")

  def valid = false

  def claims = List.empty

  def verified = List.empty
}