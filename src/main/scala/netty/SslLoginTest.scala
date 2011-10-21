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

package org.w3.readwriteweb.netty

import org.jboss.netty.handler.ssl.SslHandler
import unfiltered.request.Path
import unfiltered.response.ResponseString
import unfiltered.netty._
import org.w3.readwriteweb.netty.{NormalPlan, KeyAuth_Https}

/**
 * A very light weight plan to test SSL login using TLS renegotiation in netty.
 * This shows how easy it is to to this, and can be useful to try out different browsers' implementations
 * The certificate should only be requested of the client on going to /test/login .
 *
 * Note: to get this to work on all browsers, and if security is just less of a concern for you, you should
 * set the sun.security.ssl.allowUnsafeRenegotiation=true and sun.security.ssl.allowLegacyHelloMessages=true
 * see:
 *
 * http://download.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#workarounds
 *
 *
 * @author hjs
 * @created: 21/10/2011
 */
object SslLoginTest extends  NormalPlan {

  def certAvailable(sslh: SslHandler): String =  try {
       sslh.getEngine.getSession.getPeerCertificateChain.head.toString
     } catch {
       case e => e.getMessage
     }


  def intent = {

    case req @ Path("/test/login") => {

       req.underlying.context.getPipeline.get(classOf[org.jboss.netty.handler.ssl.SslHandler])  match {
          case sslh: SslHandler => {
            sslh.setEnableRenegotiation(true)
            sslh.getEngine.setWantClientAuth(true)
            val future = sslh.handshake()
            future.await(5000)
            val res = if (future.isDone) {
              var r ="We are in login & we have an https handler! "
              if (future.isSuccess)
                r +=  "\r\n"+"SSL handchake Successful. Did we get the certificate? \r\n\r\n"+certAvailable(sslh)
              else {
                r += "\r\n handshake failed. Cause \r\n" +future.getCause
              }
              r
            } else {
              "Still waiting for requested certificate"
            }
            ResponseString(res)
           }
          case _ =>ResponseString("We are in login but no https handler!")
       }

    }
    case req => {
      req.underlying.context.getPipeline.get(classOf[org.jboss.netty.handler.ssl.SslHandler]) match {
        case sslh: SslHandler =>  {
          ResponseString(certAvailable(sslh))
        }
        case null => ResponseString("Just a normal hello world")

      }
    }
  }


  def main(args: Array[String]) {
    new KeyAuth_Https(8443).plan(SslLoginTest).run()
  }
}