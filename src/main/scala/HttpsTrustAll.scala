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

package org.w3.readwriteweb

import java.io.File
import javax.net.ssl.X509TrustManager
import org.jsslutils.keystores.KeyStoreLoader
import org.jsslutils.sslcontext.trustmanagers.TrustAllClientsWrappingTrustManager
import org.jsslutils.sslcontext.{X509TrustManagerWrapper, X509SSLContextFactory}
import sys.SystemProperties
import scala.util.Properties.{propOrNone => getProperty, javaHome}
import unfiltered.jetty.{Ssl, Https}
import unfiltered.jetty.Server


/**
 * @author Henry Story
 * @created: 12/10/2011
 */

class HttpsTrustAll(
    override val port: Int,
    override val host: String)
extends Https(port, host) with TrustAll


/**
 * Trust all ssl connections. Authentication will be done at a different layer
 * This code is very much tied to jetty
 * It requires the following System properties to be set
 *
 *  - jetty.ssl.keyStoreType
 *  - jetty.ssl.keyStore
 *  - jetty.ssl.keyStorePassword
 *
 *  Client Auth is set to Want.
 *
 *  Authentication could be done here, allowing the code to reject broken certificates, but then
 *  the user experience would be very bad, since TLS does not give many options for explaining what the problem
 *  is.
 */
trait TrustAll extends Ssl with Server with DelayedInit {

  import scala.sys.SystemProperties._

  val patchedSslContextFactory = {
    val trustWrapper =
      new X509TrustManagerWrapper {
        def wrapTrustManager(trustManager: X509TrustManager) =
          new TrustAllClientsWrappingTrustManager(trustManager)
      }
    
    val serverCertKeyStore = {
      val keyStoreLoader = new KeyStoreLoader
      keyStoreLoader.setKeyStoreType(getProperty("jetty.ssl.keyStoreType") getOrElse "JKS")
      keyStoreLoader.setKeyStorePath(trustStorePath)
      keyStoreLoader.setKeyStorePassword(getProperty("jetty.ssl.keyStorePassword") getOrElse "password")
      keyStoreLoader.loadKeyStore()
    }
    
    val factory = new X509SSLContextFactory(
      serverCertKeyStore,
      getProperty("jetty.ssl.keyStorePassword") getOrElse sys.error("jetty.ssl.keyStorePassword not set"),
      serverCertKeyStore) //this one is not needed since our wrapper ignores all trust managers
    
    factory.setTrustManagerWrapper(trustWrapper)
    
    factory
  }

  val trustStorePath =
    getProperty("jetty.ssl.keyStore") getOrElse {
      new File(new File(javaHome), ".keystore").getAbsolutePath
    }
  
  // not tested if ok, there was a problem anyway
  def delayedInit(x: ⇒ Unit): Unit = {
    sslConn.setSslContext(patchedSslContextFactory.buildSSLContext())
    sslConn.setWantClientAuth(true)
  }

}

