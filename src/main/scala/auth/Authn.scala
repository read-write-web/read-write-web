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

package org.w3.readwriteweb.auth

import java.security.cert.X509Certificate
import javax.servlet._
import org.w3.readwriteweb._

import collection.JavaConversions._
import javax.security.auth.Subject
import java.security.PrivilegedExceptionAction
import java.util.concurrent.TimeUnit
import com.google.common.cache.{CacheBuilder, Cache, CacheLoader}

class Authn(implicit webCache: WebCache) extends Filter {
  def init(filterConfig: FilterConfig) {}

  val idCache: Cache[X509Certificate, X509Claim] =
    CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).
      build(new CacheLoader[X509Certificate, X509Claim]() {
        def load(x509: X509Certificate) = new X509Claim(x509)
    })


  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val certChain = request.getAttribute("javax.servlet.request.X509Certificate") match {
      case certs: Array[X509Certificate] => certs.toList
      case _ => Nil
    }

    val subject = new Subject()
    if (certChain.size == 0) {
      System.err.println("No certificate found!")
      subject.getPrincipals.add(Anonymous())
    } else {
      val x509c = idCache.get(certChain.get(0))
      subject.getPublicCredentials.add(x509c)
      val verified = for (
        claim <- x509c.webidclaims;
        if (claim.verified)
      ) yield claim.principal
      subject.getPrincipals.addAll(verified)
      System.err.println("Found "+verified.size+" principals: "+verified)
    }
    try {
      Subject.doAs(subject,new PrivilegedExceptionAction[Unit]() { def run(): Unit = chain.doFilter(request, response) } )
    } catch {
      case e: Exception => System.err.println("cought "+e)
    }
//    chain.doFilter(request, response)
  }

  def destroy() {}
}

