/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * under the MIT licence defined at
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

import unfiltered.spec.netty.Started
import org.specs.Specification
import unfiltered.netty.{ReceivedMessage, ServerErrorResponse, cycle}
import java.io.File
import org.w3.readwriteweb._
import grizzled.file.GrizzledFile._

import java.security.cert.X509Certificate
import org.apache.http.conn.scheme.Scheme
import dispatch.Http
import org.apache.http.client.HttpClient
import javax.net.ssl.{SSLContext, X509TrustManager, KeyManager}
import util.trySome
import java.nio.file.Files

/**
 * @author hjs
 * @created: 24/10/2011
 */


trait SecureServed extends Started {
  import org.w3.readwriteweb.netty._

  //todo: replace this with non property method of setting this.
  System.setProperty("netty.ssl.keyStore",getClass.getClassLoader.getResource("KEYSTORE.jks").getFile)
  System.setProperty("netty.ssl.keyStoreType","JKS")
  System.setProperty("netty.ssl.keyStorePassword","secret")

  def setup: (Https => Https)
  lazy val server = setup( new KeyAuth_Https(port) )


}

object AcceptAllTrustManager extends X509TrustManager {
      def checkClientTrusted(chain: Array[X509Certificate], authType: String) {}
      def checkServerTrusted(chain: Array[X509Certificate], authType: String) {}
      def getAcceptedIssuers = Array[X509Certificate]()
}

/**
 * Netty resource managed with access control enabled
 */
trait SecureResourceManaged extends Specification with SecureServed {
  import org.jboss.netty.handler.codec.http._

  def resourceManager: ResourceManager

  /**
   * Inject flexible behavior into the client ssl so that it does not
   * break on every localhost problem. It returns a key manager which can be used
   * to allow the client to take on various guises
   */
  def flexi(client: HttpClient, km: KeyManager): SSLContext = {

    val  sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
  
    sslContext.init(Array(km.asInstanceOf[KeyManager]), Array(AcceptAllTrustManager),null); // we are not trying to test our trust of localhost server

    import org.apache.http.conn.ssl._
    val sf = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
    val scheme = new Scheme("https", 443, sf);
    client.getConnectionManager.getSchemeRegistry.register(scheme)

    sslContext
  }



  val webCache = new WebCache()
  val serverSslContext = javax.net.ssl.SSLContext.getInstance("TLS");



  flexi(webCache.http.client, new FlexiKeyManager)


  val testKeyManager = new FlexiKeyManager();
  val sslContext = flexi(Http.client,testKeyManager)
  



  val rww = new cycle.Plan  with cycle.ThreadPool with ServerErrorResponse with ReadWriteWeb[ReceivedMessage,HttpResponse] {
    val rm = resourceManager
    def manif = manifest[ReceivedMessage]
    override val authz = new RDFAuthZ[ReceivedMessage,HttpResponse](webCache,resourceManager)
  }

  def setup = { _.plan(rww) }

}

trait SecureFileSystemBased extends SecureResourceManaged {
  lazy val mode: RWWMode = ResourcesDontExistByDefault

  lazy val lang = TURTLE

  lazy val baseURL = "/wiki"

  /**
   * finding where the specs2 output directory is, so that we can create temporary directories there,
   * which can then be viewed if tests are unsuccessful, but that will also be removed on "sbt clean"
   */
  lazy val outDirBase = new File(trySome { System.getProperty("spec2.outDir") } getOrElse  "target/specs2-reports/")

  lazy val root = {
    outDirBase.mkdirs()
    val dir = Files.createTempDirectory(outDirBase.toPath, "test_rww_")
    System.out.println("Temp directory: "+dir.toString)
    dir.toFile
  }

  lazy val resourceManager = new Filesystem(root, baseURL, lang)(mode)

  doBeforeSpec {
    if (root.exists) root.deleteRecursively()
    root.mkdir()
  }

}
