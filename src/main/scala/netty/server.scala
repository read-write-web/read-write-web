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


import unfiltered.netty._
import unfiltered.response.ResponseString
import unfiltered.request.Path
import org.jboss.netty.handler.ssl.SslHandler
import java.lang.String
import org.jboss.netty.channel.{ChannelPipelineFactory, ChannelHandler}
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLEngine, X509ExtendedTrustManager}
import java.net.Socket

trait nplan extends cycle.Plan with cycle.ThreadPool with ServerErrorResponse

object light extends nplan {

  def certAvailable(sslh: SslHandler): String =  try {
       sslh.getEngine.getSession.getPeerCertificateChain.head.toString
     } catch {
       case e => e.getMessage
     }


  def intent = {

    case req @ Path("/login") => {

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
    new Trusting_Https(8443).plan(light).run()
  }
}

object Https {

   /** bind to a the loopback interface only */
  def local(port: Int): Https =
    new Https(port, "127.0.0.1")

  /** bind to any available port on the loopback interface */
  def anylocal = local(unfiltered.util.Port.any)
}

case class Trusting_Https(override val port: Int) extends Https(port)  with TrustAllSsl



/** Http + Ssl implementation of the Server trait. */
class Https(val port: Int,
            val host: String,
            val handlers: List[() => ChannelHandler],
            val beforeStopBlock: () => Unit)
extends HttpServer
with TrustAllSsl { self =>

  def this(port: Int, host: String) = this(port, host, Nil, () => ())

  def this(port: Int) = this(port, "0.0.0.0")

  def pipelineFactory: ChannelPipelineFactory =
    new SecureServerPipelineFactory(channels, handlers, this)

  type ServerBuilder = Https
  def handler(h: => ChannelHandler) = new Https(port, host, { () => h } :: handlers, beforeStopBlock)
  def plan(plan: => ChannelHandler) = handler(plan)
  def beforeStop(block: => Unit) = new Https(port, host, handlers, { () => beforeStopBlock(); block })
}


/**
 * a class that trusts all ssl certificates - as long as the tls handshake crypto works of course.
 * Ie: we don't care about who signed the certificate. All we know when the certificate is received
 * is that the client knew the private key of the given public key. It is the job of other layers,
 * to follow through on claims made in the certificate.
 */
trait TrustAllSsl extends Ssl {
  
  import java.security.SecureRandom
  import javax.net.ssl.{SSLContext, TrustManager}

  val nullArray = Array[X509Certificate]()

  val trustManagers = Array[TrustManager](new X509ExtendedTrustManager {

    def checkClientTrusted(chain: Array[X509Certificate], authType: String, socket: Socket) {}

    def checkClientTrusted(chain: Array[X509Certificate], authType: String, engine: SSLEngine) {}

    def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) {}

    def checkServerTrusted(chain: Array[X509Certificate], authType: String, socket: Socket) {}

    def checkServerTrusted(chain: Array[X509Certificate], authType: String, engine: SSLEngine) {}

    def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) {}

    def getAcceptedIssuers() = nullArray
  })


  override def initSslContext(ctx: SSLContext) = ctx.init(keyManagers, trustManagers, new SecureRandom)


}