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

package org.w3.readwriteweb.auth

import java.security.cert.X509Certificate
import javax.servlet._
import org.w3.readwriteweb._

import collection.JavaConversions._
import javax.security.auth.Subject
import java.security.PrivilegedExceptionAction
import java.util.concurrent.TimeUnit
import com.google.common.cache.{CacheBuilder, Cache, CacheLoader}

/**
 * This filter places the all the principals into a Subject,
 * which can then be accessed later on in by the code.
 *
 * note: It would be better if this were only called at the point when authentication
 * is needed. That is in fact possible with TLS renegotiation, but requires a server that allows
 * access to the TLS layer. This is an intermediary solution.
 */
class AuthenticationFilter(implicit webCache: WebCache) extends Filter {
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

