/*
 * Copyright (c) 2011 World Wide Web Consortium
 * under the W3C licence defined at http://opensource.org/licenses/W3C
 */


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
import java.net.{MalformedURLException, URL}


object ReadWriteWebJetty extends ReadWriteWebArgs {

  import unfiltered.filter.Planify

  // regular Java main
  def main(args: Array[String]) {

    try {
      parser.parse(args)
      postParse
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
      case Some(port) => new HttpsTrustAll(port,host.value.getOrElse("0.0.0.0"))
      case None => Http(httpPort.value.get,host.value.getOrElse("0.0.0.0"))
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

  /**
   * set environmental variables, but adapt them for the underlying server
   * (a silly hack because jetty and netty in unfiltered use different TLS security
   * properties, and I don't want to dig through each library to set that right just now)
   * @param name
   * @param value
   */
  def setPropHack(name: String, value: String) {
    System.setProperty("jetty."+name,value)
  }
}


