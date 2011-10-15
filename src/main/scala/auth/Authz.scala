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

import unfiltered.filter.Plan
import unfiltered.Cycle
import unfiltered.request._
import unfiltered.response.{Unauthorized, BadRequest}
import collection.JavaConversions._
import javax.security.auth.Subject
import org.w3.readwriteweb.WebCache
import org.w3.readwriteweb.auth.Authz._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

/**
 * @author hjs
 * @created: 14/10/2011
 */

object HttpMethod {
   def unapply(req: HttpRequest[_]): Option[Method] =
     Some(
       req.method match {
         case "GET" => GET
         case "PUT" => PUT
         case "HEAD" => HEAD
         case "POST" => POST
         case "CONNECT" => CONNECT
         case "OPTIONS" => OPTIONS
         case "TRACE" => TRACE
         case m => new Method(m)
       })
     
   
}

object Authz {

  def authMethod(s: String, httpMethod: Method) = new AuthzFunc


  
  implicit def x509toSubject(x509c: X509Claim)(implicit cache: WebCache): Subject = {
    val subject = new Subject()
    subject.getPublicCredentials.add(x509c)
    val verified = for (
      claim <- x509c.webidclaims;
      if (claim.verified)
    ) yield claim.principal
    subject.getPrincipals.addAll(verified)
    subject
  }
}

class NoAuthZ extends Authz {
  def apply(in: Plan.Intent) = in
}

class SimpleAuthZ(implicit val WebCache: WebCache) extends Authz {

  def apply(in: Plan.Intent): Plan.Intent = {
    req: HttpRequest[HttpServletRequest] => req match {
        case Path(path) & HttpMethod(m) =>  { //first get things that cost nothing
            val autf = authMethod(path,m)
            if (autf.requiresAuth) {
               X509Claim.unapply(req) match {
                 case Some(claim) => {
                   if (autf.isAuthorized(claim,m,path)) in(req)
                   else Unauthorized
                 }
                 case None => Unauthorized
               }
            } else Unauthorized
        }
        case _ => BadRequest
      }
  }

}

trait Authz {
   def apply(in: Plan.Intent): Plan.Intent // we may want to generalise more later to  Cycle.Intent[A,B]

}

class AuthzFunc {
   def requiresAuth(): Boolean = true
   def isAuthorized(webid: Subject,  m: Method, path: String ): Boolean =
     webid.getPrincipals().exists(p=>p.getName=="http://bblfish.net/people/henry/card#me")
   
}
