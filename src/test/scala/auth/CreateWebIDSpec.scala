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
import java.security.cert.X509Certificate
import java.security._
import interfaces.RSAPublicKey
import java.net.{Socket, URL}
import scala.collection.mutable
import javax.net.ssl._
import java.io.File
import org.w3.readwriteweb.{Post, RDFXML, TURTLE}
import org.apache.commons.codec.binary.Hex


/**
 * A key manager that can contain multiple keys, but where the client can take one of a number of identities
 * One at a time - so this is not synchronised. It also assumes that the server will accept all CAs, which in
 * these test cases it does.
 */
class FlexiKeyManager extends X509ExtendedKeyManager {
  val keys = mutable.Map[String, (Array[X509Certificate], PrivateKey)]()
  
  def addClientCert(alias: String,certs: Array[X509Certificate], privateKey: PrivateKey) {
    keys += (alias -> (certs -> privateKey))
  }
  
  var currentId: String = null
  
  def setId(alias: String) { currentId = if (keys.contains(alias)) alias else null }
  
  def getClientAliases(keyType: String, issuers: Array[Principal]) = if (currentId!=null) Array(currentId) else null
  
  def chooseClientAlias(keyType: Array[String], issuers: Array[Principal], socket: Socket) = currentId

  def getServerAliases(keyType: String, issuers: Array[Principal]) = null

  def chooseServerAlias(keyType: String, issuers: Array[Principal], socket: Socket) = ""

  def getCertificateChain(alias: String) = keys.get(alias) match { case Some(certNKey) => certNKey._1; case None => null}

  def getPrivateKey(alias: String) = keys.get(alias).map(ck=>ck._2).getOrElse(null)

  override def chooseEngineClientAlias(keyType: Array[String], issuers: Array[Principal], engine: SSLEngine): String = currentId
}

/**
 * @author hjs
 * @created: 23/10/2011
 */

object CreateWebIDSpec extends SecureFileSystemBased {
  lazy val peopleDirUri = host / "wiki/people/"
  lazy val webidProfileDir = peopleDirUri / "Lambda/"
  lazy val webidProfile = webidProfileDir / "Joe"
  lazy val joeProfileOnDisk = new File(root,"people/Lambda/Joe")
  lazy val lambdaMetaURI = webidProfileDir/".meta.n3"

  lazy val directory = new File(root, "people")
  lazy val lambdaDir = new File(directory,"Lambda")
  lazy val lambdaMeta = new File(lambdaDir,".meta.n3")



  val foaf = """
       @prefix foaf: <http://xmlns.com/foaf/0.1/> .
       @prefix : <#> .

       <> a foaf:PersonalProfileDocument;
          foaf:primaryTopic :me .

       :jL a foaf:Person;
           foaf:name "Joe Lambda"@en .
  """

  val webID = new URL(webidProfile.secure.to_uri + "#jL")


  val updatePk = """
       PREFIX cert: <http://www.w3.org/ns/auth/cert#>
       PREFIX xsd:    <http://www.w3.org/2001/XMLSchema#>
       PREFIX : <#>
       INSERT DATA {
         :jL cert:key [ cert:modulus "%s"^^xsd:hexBinary;
                        cert:exponent "%s"^^xsd:integer ] .
       }
  """

  val updateFriend = """
       PREFIX foaf: <http://xmlns.com/foaf/0.1/>
       PREFIX : <#>
       INSERT DATA {
          :jL foaf:knows <%s> .
       }
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


   "POSTing public key into the /people/Lambda/Joe profile" should {

     "first create signed WebID certificate and add it to local SSL keystore" in {
       val keystore = getClass.getClassLoader.getResource("KEYSTORE.jks")
       val signer = X509CertSigner(
         keystore,
         "JKS",
         "secret",
         "selfsigned"
       )

       val rsagen = KeyPairGenerator.getInstance("RSA")
       rsagen.initialize(512)
       val rsaKP = rsagen.generateKeyPair()


       val testCert = signer.generate("CN=JoeLambda, OU=DIG, O=W3C", rsaKP.getPublic.asInstanceOf[RSAPublicKey], 1, webID)
       
       testCert mustNotBe null

       testKeyManager.addClientCert("JoeLambda",Array(testCert),rsaKP.getPrivate)
    }

     "return a 200 when POSTing relations to profile" in {
       val joeCert = testKeyManager.getCertificateChain("JoeLambda")

       joeCert mustNotBe null

       val joeKey = joeCert(0).getPublicKey.asInstanceOf[RSAPublicKey]

       val hex = new String(Hex.encodeHex(joeKey.getModulus.toByteArray))
       val updateQStr = updatePk.format(
                     hex.stripPrefix("00"),
                     joeKey.getPublicExponent()
       )

       val httpCode = Http(
         webidProfile.secure.postSPARQL(updateQStr) get_statusCode )
        httpCode must_== 200
     }

     "create 3 more relations" in {
       val model = Http(webidProfile.secure as_model(baseURI(webidProfile.secure), RDFXML))
       model.size() must_== 7
         
     }
   }

  val aclRestriction = """
  @prefix acl: <http://www.w3.org/ns/auth/acl#> .
  @prefix foaf: <http://xmlns.com/foaf/0.1/> .
  @prefix : <#> .

  :a1 a acl:Authorization;
     acl:accessTo <Joe>;
     acl:mode acl:Write;
     acl:agent <%s>, <http://bblfish.net/people/henry/card#me> .

  :allRead a acl:Authorization;
     acl:accessTo <Joe>;
     acl:mode acl:Read;
     acl:agentClass foaf:Agent .
  """


  "PUT access control statements in directory" should {
    "return a 201" in {
      val httpCode = Http( lambdaMetaURI.secure.put(TURTLE, aclRestriction.format(webID.toExternalForm)) get_statusCode )
       httpCode must_== 201
    }

    "create a resource on disk" in {
       lambdaMeta must be file
    }
    
    "everybody can still read the profile" in {
      testKeyManager.setId(null)
      val model = Http(webidProfile.secure as_model(baseURI(webidProfile.secure), RDFXML))
      model.size() must_== 7
    }
    
    "no one other than the user can change the profile" in {
      val httpCode = Http.when(_ == 401)(webidProfile.secure.put(TURTLE, foaf) get_statusCode)
      httpCode must_== 401
    }

    "access it as the user - allow him to add a friend" in {
      testKeyManager.setId("JoeLambda")

/*    The code below was very useful to help me debug this.
      Sometimes it helps to get back to basics. So I will leave this here.

      val scon =webidProfile.secure.to_uri.toURL.openConnection().asInstanceOf[HttpsURLConnection]
      scon.setSSLSocketFactory(sslContext.getSocketFactory)
      scon.setRequestProperty("Content-Type",Post.SPARQL)
      scon.setRequestProperty("User-Agent" , "Java/1.7.0")
      scon.setRequestMethod("POST")
      val msg = updateFriend.format("http://bblfish.net/#hjs").getBytes("UTF-8")
      scon.setRequestProperty("Content-Length",msg.length.toString)
      scon.setDoOutput(true)
      scon.setDoInput(true)

      val out = scon.getOutputStream
      out.write(msg)
      out.flush()
      out.close()
      scon.connect()

      val httpCode = scon.getResponseCode
*/

      val req =webidProfile.secure.PUT <:< Map("User-Agent" -> "Java/1.7.0","Content-Type"->Post.SPARQL)
      val req2 = req.copy(
              method="POST",
              body=Some(new RefStringEntity(updateFriend.format(webID.toExternalForm),Post.SPARQL,"UTF-8"))
            )

      val httpCode = Http( req2 get_statusCode )
      httpCode must_== 200
    }

    "and so have one more relation in the foaf" in {
      val model = Http(webidProfile.secure as_model(baseURI(webidProfile.secure), RDFXML))
      model.size() must_== 8
    }

  }



}