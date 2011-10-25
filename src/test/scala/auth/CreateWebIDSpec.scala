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

import org.w3.readwriteweb.utiltest._

import dispatch._
import org.w3.readwriteweb.TURTLE
import java.security.KeyStore
import java.io.{FileInputStream, File}
import org.apache.http.conn.scheme.Scheme
import javax.net.ssl.{X509TrustManager, TrustManager, TrustManagerFactory}
import java.security.cert.X509Certificate
import java.lang.String

/**
 * @author hjs
 * @created: 23/10/2011
 */

object CreateWebIDSpec extends SecureFileSystemBased {
  lazy val peopleDirUri = host / "wiki/people/"
  lazy val webidProfileDir = peopleDirUri / "Lambda/"
  lazy val webidProfile = webidProfileDir / "Joe"
  lazy val joeProfileOnDisk = new File(root,"people/Lambda/Joe")

  lazy val directory = new File(root, "people")
  lazy val lambdaDir = new File(directory,"Lambda")

{
  val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
  val  sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
  sslContext.init(null, Array[TrustManager](new X509TrustManager {
    def checkClientTrusted(chain: Array[X509Certificate], authType: String) {}
    def checkServerTrusted(chain: Array[X509Certificate], authType: String) {}
    def getAcceptedIssuers = Array[X509Certificate]()
  }),null); // we are not trying to test our trust of localhost server
  val sf = new org.apache.http.conn.ssl.SSLSocketFactory(sslContext)
  val  scheme = new Scheme("https", sf, 443);
  Http.client.getConnectionManager.getSchemeRegistry.register(scheme)
}


  val foaf = """
       @prefix foaf: <http://xmlns.com/foaf/0.1/> .
       @prefix : <#> .

       <> a foaf:PersonalProfileDocument;
          foaf:primaryTopic :me .

       :jl a foaf:Person;
           foaf:name "Joe Lambda"@en .
  """

  
  "PUTing nothing on /people/" should {
       "return a 201" in {
         val httpCode = Http(peopleDirUri.secure.put(TURTLE, "") get_statusCode)
         httpCode must_== 201
       }
       "create a directory on disk" in {
         directory must be directory
       }
   }
  
  
  "PUTing nothing on /people/Lambda/" should { // but should it really? Should it not create a resource too? Perhaps index.html?
     "return a 201" in {
       val httpCode = Http(webidProfileDir.secure.put(TURTLE, "") get_statusCode)
       httpCode must_== 201
     }
     "create a directory on disk" in {
       lambdaDir must be directory
     }
   }
  
  
   "PUTing a WebID Profile on /people/Lambda/" should {
     "return a 201" in {
       val httpCode = Http( webidProfile.secure.put(TURTLE, foaf) get_statusCode )
        httpCode must_== 201
     }
     "create a resource on disk" in {
        joeProfileOnDisk must be file
     }
   }
}