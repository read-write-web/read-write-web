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



import org.slf4j.LoggerFactory
import java.security.cert.X509Certificate
import org.w3.readwriteweb.WebCache
import javax.security.auth.Refreshable
import java.util.Date
import collection.JavaConversions._


/**
 * @author hjs
 * @created: 13/10/2011
 */

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
class X509Claim(val cert: X509Certificate)(implicit webCache: WebCache) extends Refreshable  {

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

