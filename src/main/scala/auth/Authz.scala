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
import unfiltered.request._
import unfiltered.response.Unauthorized
import collection.JavaConversions._
import javax.security.auth.Subject
import javax.servlet.http.HttpServletRequest
import java.net.URL
import org.w3.readwriteweb.{Resource, ResourceManager, WebCache}
import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryExecution, QuerySolutionMap, QueryFactory}
import sun.management.resources.agent


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

object AuthZ {


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

object NullAuthZ extends AuthZ {
  def subject(req: NullAuthZ.HSRequest) = null

  def guard(m: Method, path: String) = null

  override def protect(in: Plan.Intent) = in
}


abstract class AuthZ {
  type HSRequest = HttpRequest[HttpServletRequest]
  // I need a guard
  //   - in order to be able to have different implementations, but subclassing could do to
  //   - the guard should get the information from the file system or the authdb, so it should know where those are

  // I will need a web cache to get the subject


  def protect(in: Plan.Intent): Plan.Intent = {
    req: HSRequest => req match {
      case HttpMethod(method) & Path(path) if guard(method, path).allow(() => subject(req)) => in(req)
      case _ => Unauthorized
    }
  }

  def subject(req: HSRequest): Option[Subject]

  /** create the guard defined in subclass */
  def guard(m: Method, path: String): Guard

  abstract class Guard(m: Method, path: String) {

    /**
     * verify if the given request is authorized
     * @param subj function returning the subject to be authorized if the resource needs authorization
     */
    def allow(subj: () => Option[Subject]): Boolean
  }

}


class RDFAuthZ(val webCache: WebCache, rm: ResourceManager) extends AuthZ {
  import AuthZ.x509toSubject
  implicit val cache : WebCache = webCache

  def subject(req: HSRequest) = req match {
    case X509Claim(claim) => Option(claim)
    case _ => None
  }

  object RDFGuard {
    val acl = "http://www.w3.org/ns/auth/acl#"
    val Read = acl+"Read"
    val Write = acl+"Write"
    val Control = acl+"Control"

    val selectQuery = QueryFactory.create("""
    		  PREFIX acl: <http://www.w3.org/ns/auth/acl#>
    		  SELECT ?mode ?group ?agent
    		  WHERE {
              ?auth acl:accessTo ?res ;
                    acl:mode ?mode .
          OPTIONAL { ?auth acl:agentClass ?group . }
	        OPTIONAL { ?auth acl:agent ?agent . }
    		  }""")
  }

  def guard(method: Method, path: String) = new Guard(method, path) {
    import RDFGuard._
    import org.w3.readwriteweb.util.wrapValidation
    import org.w3.readwriteweb.util.ValidationW


    def allow(subj: () => Option[Subject]) = {
      val resurl = "file://local"+path + ".protect.n3"
      val r: Resource = rm.resource(new URL(resurl))
      val res: ValidationW[Boolean,Boolean] = for {
        model <- r.get() failMap { x => true }
      } yield {
        val initialBinding = new QuerySolutionMap();
        initialBinding.add("res", model.createResource("file://local"+path))
        val qe: QueryExecution = QueryExecutionFactory.create(selectQuery, model)//, initialBinding)
        val agentsAllowed = try {
          val exec = qe.execSelect()
          val res = for (qs <- exec) yield {
            val methods = qs.get("mode").toString match {
              case Read => List(GET)
              case Write => List(PUT, POST)
              case Control => List(POST)
              case _ => List(GET, PUT, POST, DELETE) //nothing everything is allowed
            }
            if (methods.contains(method)) Some(Pair(qs.get("agent"), qs.get("group")))
            else None
          }
          res.flatten.toList
        } finally {
          qe.close()
        }
        if (agentsAllowed.size>0) {
          subj() match {
            case Some(s) => agentsAllowed.exists{ 
              p =>  s.getPrincipals(classOf[WebIdPrincipal]).
                exists(id=> {
                val ps = if (p._1 != null) p._1.toString else null;
                ps == id.webid
              })
            }
            case None => false
          }
          //currently we just check for agent match. Group match would require us to have a store
          //of trusted information of which groups someone was member of -- one would probably need a reasoning engine there.
        } else false //
      }
      res.validation.fold()
    } // end allow()


  }

}


class ResourceGuard(path: String, reqMethod: Method) {


  def allow(subjFunc: () => Option[Subject]) = {
    subjFunc().isEmpty
  }
}



