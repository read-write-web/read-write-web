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

import javax.servlet.http.HttpServletRequest
import unfiltered.request.HttpRequest
import unfiltered.netty.ReceivedMessage
import java.security.cert.{X509Certificate}
import java.security.cert.Certificate
import java.util.Date
import java.math.BigInteger
import java.security.{SecureRandom, KeyPair}
import java.net.URL
import sun.security.x509._

object X509Cert {
  val WebID_DN="""O=FOAF+SSL, OU=The Community of Self Signers, CN=Not a Certification Authority"""

  /**
   * Adapted from http://bfo.com/blog/2011/03/08/odds_and_ends_creating_a_new_x_509_certificate.html
   * The libraries used here are sun gpled code. This is much lighter to use than bouncycastle. All VMs that already
   * have these classes don't need to download the code. It should be easy in scala to create a build that can decide
   * if these need to be added to the classpath. I think the code just looks better than bouncycastle too.
   *
   * Create a self-signed X.509 Certificate
   * @param issuerDN the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
   * @param pair the KeyPair
   * @param days how many days from now the Certificate is valid for
   * @param algorithm the signing algorithm, eg "SHA1withRSA"
   */
    def generate_self_signed(issuerDN: String,
                 pair: KeyPair,
                 days: Int,
                 webId: URL,
                 algorithm: String="SHA1withRSA"): X509Certificate = {
      var info = new X509CertInfo
      val from = new Date
      val to = new Date(from.getTime + days*24*60*60*1000) 
      val interval = new CertificateValidity(from, to)
      val sn = new BigInteger(64, new SecureRandom)
      val owner = new X500Name(issuerDN)
      info.set(X509CertInfo.VALIDITY, interval)
      info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn))
      info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner))
      info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner))
      info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic))
      info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3))
      val extensions = new CertificateExtensions();
      val san = new SubjectAlternativeNameExtension(new GeneralNames().add(new GeneralName(new URIName(webId.toExternalForm))))
      extensions.set(san.getName,san)
      info.set(X509CertInfo.EXTENSIONS,extensions)
      val algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid)
      info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo))
      var cert = new X509CertImpl(info)
      cert.sign(pair.getPrivate, algorithm)
      val sigAlgo = cert.get(X509CertImpl.SIG_ALG).asInstanceOf[AlgorithmId]
      info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, sigAlgo)
      cert = new X509CertImpl(info)
      cert.sign(pair.getPrivate, algorithm)
      return cert
    }


}


object Certs {


  def unapplySeq[T](r: HttpRequest[T])(implicit m: Manifest[T]): Option[IndexedSeq[Certificate]] = {
    if (m <:< manifest[HttpServletRequest]) unapplyServletRequest(r.asInstanceOf[HttpRequest[HttpServletRequest]])
    else if (m <:< manifest[ReceivedMessage]) unapplyReceivedMessage(r.asInstanceOf[HttpRequest[ReceivedMessage]])
    else None //todo: should  throw an exception here?
  }


  //todo: should perhaps pass back error messages, which they could in the case of netty

  private def unapplyServletRequest[T <: HttpServletRequest](r: HttpRequest[T]):
  Option[IndexedSeq[Certificate]] = {
    r.underlying.getAttribute("javax.servlet.request.X509Certificate") match {
      case certs: Array[Certificate] => Some(certs)
      case _ => None
    }
  }

  private def unapplyReceivedMessage[T <: ReceivedMessage](r: HttpRequest[T]):
  Option[IndexedSeq[Certificate]] = {

    import org.jboss.netty.handler.ssl.SslHandler
    r.underlying.context.getPipeline.get(classOf[SslHandler]) match {
      case sslh: SslHandler => try {
        //return the client certificate in the existing session if one exists
        Some(sslh.getEngine.getSession.getPeerCertificates)
      } catch {
        case e => {
          // request a certificate from the user
          sslh.setEnableRenegotiation(true)
          sslh.getEngine.setWantClientAuth(true)
          val future = sslh.handshake()
          future.await(30000) //that's certainly way too long.
          if (future.isDone) {
            if (future.isSuccess) try {
              Some(sslh.getEngine.getSession.getPeerCertificates)
            } catch {
              case e => None
            } else {
              None
            }
          } else {
            None
          }
        }
      }
      case _ => None
    }

  }
}

