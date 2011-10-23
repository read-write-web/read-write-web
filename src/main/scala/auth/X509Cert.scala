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

