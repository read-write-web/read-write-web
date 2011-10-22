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
import org.w3.readwriteweb.WebCache
import javax.security.auth.Refreshable
import java.util.Date
import collection.JavaConversions._
import java.util.concurrent.TimeUnit
import com.google.common.cache.{CacheLoader, CacheBuilder, Cache}
import javax.servlet.http.HttpServletRequest
import unfiltered.request.HttpRequest

/**
 * @author hjs
 * @created: 13/10/2011
 */

object X509Claim {
  final val logger = LoggerFactory.getLogger(classOf[X509Claim])

  val idCache: Cache[X509Certificate, X509Claim] =
     CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).
       build(new CacheLoader[X509Certificate, X509Claim]() {
         def load(x509: X509Certificate) = new X509Claim(x509)
     })

  def unapply[T](r: HttpRequest[T])(implicit webCache: WebCache,m: Manifest[T]): Option[X509Claim] =
    r match {
      case Certs(c1: X509Certificate, _*) => Some(idCache.get(c1))
      case _ => None
    }



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
class X509Claim(val cert: X509Certificate) extends Refreshable  {

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

}

