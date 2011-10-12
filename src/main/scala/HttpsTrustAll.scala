/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms are permitted
 * provided that the above copyright notice and this paragraph are
 * duplicated in all such forms and that any documentation,
 * advertising materials, and other materials related to such
 * distribution and use acknowledge that the software was developed
 * by Henry Story.  The name of bblfish.net may not be used to endorse
 * or promote products derived
 * from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.w3.readwriteweb

import java.io.File
import javax.net.ssl.X509TrustManager
import org.jsslutils.keystores.KeyStoreLoader
import org.jsslutils.sslcontext.trustmanagers.TrustAllClientsWrappingTrustManager
import org.jsslutils.sslcontext.{X509TrustManagerWrapper, X509SSLContextFactory}
import sys.SystemProperties
import unfiltered.jetty.{Ssl, Https}




/**
 * @author Henry Story
 * @created: 12/10/2011
 */

case class HttpsTrustAll(override val port: Int, override val host: String) extends Https(port, host) with TrustAll


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
trait TrustAll { self: Ssl =>
   import scala.sys.SystemProperties._

   lazy val sslContextFactory = new X509SSLContextFactory(
               serverCertKeyStore,
               tryProperty("jetty.ssl.keyStorePassword"),
               serverCertKeyStore); //this one is not needed since our wrapper ignores all trust managers

   lazy val trustWrapper = new X509TrustManagerWrapper {
     def wrapTrustManager(trustManager: X509TrustManager) = new TrustAllClientsWrappingTrustManager(trustManager)
   }

   lazy val serverCertKeyStore = {
      val keyStoreLoader = new KeyStoreLoader
   		keyStoreLoader.setKeyStoreType(System.getProperty("jetty.ssl.keyStoreType","JKS"))
   		keyStoreLoader.setKeyStorePath(trustStorePath)
   		keyStoreLoader.setKeyStorePassword(System.getProperty("jetty.ssl.keyStorePassword","password"))
      keyStoreLoader.loadKeyStore();
   }

   sslContextFactory.setTrustManagerWrapper(trustWrapper);


 	 lazy val trustStorePath =  new SystemProperties().get("jetty.ssl.keyStore") match {
       case Some(path) => path
       case None => new File(new File(tryProperty("user.home")), ".keystore").getAbsolutePath
   }

   sslConn.setSslContext(sslContextFactory.buildSSLContext())
   sslConn.setWantClientAuth(true)

}

