/*
 * Copyright (c) 2011 World Wide Web Consortium
 * under the W3C licence defined at http://opensource.org/licenses/W3C
 */

package org.w3.readwriteweb

import java.io.File
import java.net.{MalformedURLException, URL}
import org.clapper.argot.{ArgotConversionException, ArgotParser, ArgotConverters}
import org.slf4j.{LoggerFactory, Logger}
import org.w3.readwriteweb.auth.X509CertSigner


/**
 * @author bblfish
 * @created 22/05/2012
 */

trait ReadWriteWebArgs {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  // in Order to be receptive to DNS changes the DNS cache properties below must be set
  // please tune them to see what works best
  java.security.Security.setProperty("networkaddress.cache.ttl" , ""+60*10);
  java.security.Security.setProperty("networkaddress.cache.negative.ttl",""+60*3) //3 minutes
  import ArgotConverters._

  val postUsageMsg= Some(
    """
      |NOTES
      |
      |  - One of --http or --https must be selected
      |  - Trust stores are not needed because we use the WebID protocol, and client certs are nearly never signed by CAs
      |
      |SYSTEM PROPERTIES
      |
      |  You can also set the following system properties with -Dproperty=value
      |
      |  SECURITY:
      |  * ssl.keyStore: location of keystore (also --ks )
      |  * ssl.keyStorePassword: keystore password (so you don't need to type it on command line)
      |  * ssl.keyStoreType: keystore type (eg. JKS) where the server sertificate is located
      |
      |EXAMPLES
      |
      | from sbt shell in the source directory
      | > run --lang turtle --keyStore src/test/resources/KEYSTORE.jks --ksPass secret --https 8443 test_www /2012/
      |
    """.stripMargin);

  val parser = new ArgotParser("read-write-web",
    outputWidth = 120,
    preUsage = Some("Version 0.2"), postUsage = postUsageMsg,
    sortUsage = false
  )

  val mode = parser.flag[RWWMode](List("wiki"), List("strict"),
    "all resources already exist in --wiki mode (you can GET them) but not in --strict mode") {
    (sValue, opt) =>
      sValue match {
        case true => AllResourcesAlreadyExist
        case false => ResourcesDontExistByDefault
        case _ => throw new ArgotConversionException("Option %s: must be either wiki or strict" format (opt.name, sValue))
      }
  }

  val host = parser.option[String](List("host"),"domain","the domain of your machine (localhost otherwise) - not sure where this ends up revealing itself")

  val rdfLanguage = parser.option[Lang](List("language","lang"), "l", "RDF language files are stored as: turtle or rdfxml") {
    (sValue, opt) =>
      sValue match {
        case "n3" => N3
        case "turtle" => TURTLE
        case "rdfxml" => RDFXML
        case _ => throw new ArgotConversionException("Option %s: must be either n3, turtle or rdfxml" format (opt.name, sValue))
      }
  }


  /**
   * set environmental variables, but adapt them for the underlying server
   * (a silly hack because jetty and netty in unfiltered use different TLS security
   * properties, and I don't want to dig through each library to set that right just now)
   * @param name
   * @param value
   */
  def setPropHack(name: String, value: String)

  private val keyStoreOpt = parser.option[String](List("keyStore","ks"),"url",
    "path to keystore (same as system property ssl.keyStore )")

  //this should only be called after parsing
  //todo: The ArgotParsers options cannot deal with default values, which is why we have to do this ugly hack
  //it would require some mothod like orElse
  lazy val keyStore =  keyStoreOpt.value.orElse{
    Option(System.getProperty("ssl.keyStore"))
  }.map(p => new URL(new File(".").toURI.toURL,p))


  private val keyStoreTypeOpt = parser.option[String](List("ksType"),"t","Type of KeyStore (JKS by default)") {
    (sValue, opt) =>
      sValue match {
        case tp: String => tp
        case _ => "JKS"
      }
  }

  //this should only be called after parsing
  lazy val keyStoreType =  keyStoreTypeOpt.value.orElse{
    Option(System.getProperty("ssl.keyStoreType"))
  }

  val keyStoreAlias = parser.option[String](List("ksAlias"),"name","alias for the key in the keystore")

  private val keyStorePasswordOpt = parser.option[String](List("ksPass"),"pass",
    "password for keystore (better set ssl.keyStorePassword System property)")

  lazy val keyStorePassword = keyStorePasswordOpt.value.orElse{
    Option(System.getProperty("ssl.keyStorePassword"))
  }

  val clientTLSsecurity = parser.option[Boolean](List("clientTLS"),"mode",
    """client TLS connection security mode:
      | * secure: if server certificate is not signed by well known CA don't accept
      | * noCA: if the server certificate is not signed by well known CA ignore and continue
      | * noDomain: for test situations where the server cert does not name its server correctly
      | * [todo: add more flexible server certificate verification mechanisms]
    """.stripMargin) {
    (sValue, opt) =>
      sValue match {
        case "noCA" => {
          //todo: work with system property as a hack for the moment, as passing around conexts is going to require
          //      a lot of rewriting
          System.setProperty("rww.clientTLSsecurity","noCA")
          false
        }
        case "noDomain" => {
          System.setProperty("rww.clientTLSsecurity","noDomain")
          false
        }
        case _ => {
          System.setProperty("rww.clientTLSsecurity","secure")
          true
        }
      }
  }

  parser.option[Unit](List("sslReneg"),"type",
    """enable workaround for SSL regegotiation issue RFC5746 (see http://tinyurl.com/d9ydrnw )
      | * sslUnsafe: allow unsafe renegotiation (will work with older browsers)
      | * sslLegacy: allow legacy handshake without RFC 5746 messages
      | (otherwise will use the default of the JVM used)
    """.stripMargin) {
    (sValue, opt) =>
      sValue match {
        case "sslUnsafe" => System.setProperty("sun.security.ssl.allowUnsafeRenegotiation","true")
        case "sslLegacy" => System.setProperty("sun.security.ssl.allowLegacyHelloMessages","true")
        case _ => ()
      }
  }

  parser.flag[Unit](List("sslDebug"),"trace all SSL messages to output") {
    (select, opt) => if (select) System.setProperty("javax.net.debug","all")
  }

  val proxy = parser.option[URL](List("proxy"),"p",
    "Proxy to use when making client http(s) connections (sets http.proxy system property)") {
    (sValue, opt) =>
      try {
        val proxy = new URL(sValue);
        System.getProperties().put("http.proxy", proxy);
        proxy
      } catch {
        case e: MalformedURLException => throw new ArgotConversionException("Option %s: url does not parse correctly "
          format (opt.name, sValue))
      }
  }

  val httpPort = parser.option[Int]("http", "port","start the http server on port")
  val httpsPort = parser.option[Int]("https","port","start the https server on port")

  val rootDirectory = parser.parameter[File]("rootDirectory", "path to root directory where files are served", false) {
    (sValue, opt) => {
      val file = new File(sValue)
      if (! file.exists)
        throw new ArgotConversionException("Option %s: %s must be a valid path" format (opt.name, sValue))
      else
        file
    }
  }

  lazy val signer =
    X509CertSigner( keyStore, keyStoreType,
      keyStorePassword,  keyStoreAlias.value.orElse(Some("selfsigned")) )

  val baseURL = parser.parameter[String]("baseURL", "the base path in the URL from which resources will be served", false)

  /** execute after parseing command line */
  protected def postParse {
    keyStore.map(u=>setPropHack("ssl.keyStore",u.getPath))
    keyStorePassword.map(setPropHack("ssl.keyStorePassword",_))
    keyStoreType.map(setPropHack("ssl.keyStoreType",_))
  }

}
