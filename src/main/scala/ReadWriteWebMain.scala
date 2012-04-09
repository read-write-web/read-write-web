package org.w3.readwriteweb

import auth.{X509CertSigner, RDFAuthZ, X509view}
import org.w3.readwriteweb.util._

import unfiltered.jetty._
import Console.err
import org.slf4j.{Logger, LoggerFactory}

import org.clapper.argot._
import ArgotConverters._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.lang.String
import java.io.File

trait ReadWriteWebArgs {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  // in Order to be receptive to DNS changes the DNS cache properties below must be set
  // please tune them to see what works best
  java.security.Security.setProperty("networkaddress.cache.ttl" , ""+60*10);
  java.security.Security.setProperty("networkaddress.cache.negative.ttl",""+60*3) //3 minutes


  val postUsageMsg= Some("""
  |PROPERTIES
  |
  | * Keystore properties that need to be set if https is started
  |  -Dnetty.ssl.keyStoreType=type : the type of the keystore, JKS by default usually
  |  -Dnetty.ssl.keyStore=path : specify path to key store (for https server certificate)
  |  -Dnetty.ssl.keyStorePassword=password : specify password for keystore store (optional)
  |  (for jetty, replace "netty" with "jetty")
  |
  | * application arguments:
  |  --http  start server as plain http server
  |  --https start server as in secured mode using https (TLS)
  |  --language [turtle, rdfxml] save RDF in one of the given formats on disk
  |  --clientTLS [secure, insecure] client connections abide by CA verification
  |   * secure : if server certificate is not signed by well known CA don't accept
  |   * insecure: if the server certificate is not signed by well known CA ignore and continue
  |   * [todo: add more flexible server certificate verification mechanisms]
  |
  |NOTES
  |
  |  - Trust stores are not needed because we use the WebID protocol, and client certs are nearly never signed by CAs
  |  - one of --http or --https must be selected
  |
     """.stripMargin);

  val parser = new ArgotParser("read-write-web",postUsage=postUsageMsg)

  val mode = parser.option[RWWMode](List("mode"), "m", "wiki mode: wiki or strict") {
    (sValue, opt) =>
      sValue match {
        case "wiki" => AllResourcesAlreadyExist
        case "strict" => ResourcesDontExistByDefault
        case _ => throw new ArgotConversionException("Option %s: must be either wiki or strict" format (opt.name, sValue))
      }
    }

  val rdfLanguage = parser.option[Lang](List("language"), "l", "RDF language") {
    (sValue, opt) =>
      sValue match {
        case "n3" => N3
        case "turtle" => TURTLE
        case "rdfxml" => RDFXML
        case _ => throw new ArgotConversionException("Option %s: must be either n3, turtle or rdfxml" format (opt.name, sValue))
      }
  }

  val clientTLSsecurity = parser.option[Boolean](List("clientTLS"),"c","client TLS connection security level") {
    (sValue, opt) =>
      sValue match {
        case "insecure" => {
          //todo: work with system property as a hack for the moment, as passing around conexts is going to require
          //      a lot of rewriting
          System.setProperty("rww.clientTLSsecurity","insecure")
          false
        }
        case _ => {
          System.setProperty("rww.clientTLSsecurity","secure")
          true
        }
      }
  }

  val httpPort = parser.option[Int]("http", "Port","start the http server on port")
  val httpsPort = parser.option[Int]("https","port","start the https server on port")

  val rootDirectory = parser.parameter[File]("rootDirectory", "root directory", false) {
    (sValue, opt) => {
      val file = new File(sValue)
      if (! file.exists)
        throw new ArgotConversionException("Option %s: %s must be a valid path" format (opt.name, sValue))
      else
        file
    }
  }

  val signer = {
    val keystore = new File(System.getProperty( "netty.ssl.keyStore")).toURI.toURL
    val ksTpe = System.getProperty("netty.ssl.keyStoreType","JKS")
    val ksPass = System.getProperty("netty.ssl.keyStorePassword")
    val alias = System.getProperty("netty.ssl.keyAlias","selfsigned")
    X509CertSigner( keystore, ksTpe, ksPass,  alias )
  }

  val baseURL = parser.parameter[String]("baseURL", "base URL", false)

}



object ReadWriteWebMain extends ReadWriteWebArgs {

  import unfiltered.filter.Planify

  // regular Java main
  def main(args: Array[String]) {

    try {
      parser.parse(args)
    } catch {
      case e: ArgotUsageException => err.println(e.message); sys.exit(1)
    }


    val filesystem =
      new Filesystem(
        rootDirectory.value.get,
        baseURL.value.get,
        lang=rdfLanguage.value getOrElse RDFXML)(mode.value getOrElse ResourcesDontExistByDefault)
    
    val rww = new ReadWriteWeb[HttpServletRequest,HttpServletResponse] {
      val rm = filesystem
      def manif = manifest[HttpServletRequest]
      override implicit val authz = new RDFAuthZ[HttpServletRequest,HttpServletResponse](filesystem)
    }


    //this is incomplete: we should be able to start both ports.... not sure how to do this yet.
    val service = httpsPort.value match {
      case Some(port) => new HttpsTrustAll(port,"0.0.0.0")
      case None => Http(httpPort.value.get)
    }

    // configures and launches a Jetty server
    service.filter(new FilterLogger(logger)).
      context("/public"){ ctx:ContextBuilder =>
        ctx.resources(ClasspathUtils.fromClasspath("public/").toURI.toURL)
    }.filter(Planify(JettyEchoPlan.intent)).
      filter(Planify(rww.intent)).
      filter(Planify(x509v.intent)).
      run()
    
  }



  object x509v extends X509view[HttpServletRequest,HttpServletResponse] {
    def manif = manifest[HttpServletRequest]
  }

}


