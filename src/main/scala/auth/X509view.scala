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

import javax.security.auth.Subject
import java.security.{PrivilegedExceptionAction, PrivilegedActionException, AccessController}
import unfiltered.response.{Html, ContentType, Ok}
import unfiltered.request.Path
import collection.JavaConversions._

/**
 * This plan just described the authentication information.
 * This is a simple version. A future version will show EARL output, and so be useful for debugging the endpoint.
 *
 * @author hjs
 * @created: 13/10/2011
 */

object X509view {
    val plan = unfiltered.filter.Planify {
      case req @ Path(path) if path startsWith "/test/authinfo"=> {
        val context = AccessController.getContext
        val subj = try {
          AccessController.doPrivileged(new PrivilegedExceptionAction[Option[Subject]] {
            def run = Option(Subject.getSubject(context))
          })
        } catch {
          case ex: PrivilegedActionException => {
            ex.getCause match {
              case runE: RuntimeException => throw runE
              case e => {
                System.out.println("error " + e)
                None
              }
            }
          }
          case _ => None
        }
        Ok ~> ContentType("text/html") ~> Html(<html><head><title>Authentication Page</title></head>
          <body><h1>Authentication Info received</h1>
            {subj match {
          case Some(x) => <span><p>You were identified with the following WebIDs</p>
             <ul>{x.getPrincipals.map(p=> <li>{p}</li>)}</ul>
            {val certs = x.getPublicCredentials(classOf[X509Claim])
            if (certs.size() >0) <span><p>You sent the following certificate</p>
            <pre>{certs.head.cert.toString}</pre>
            </span> 
            }
          </span>
          case None => <p>We received no Authentication information</p>
        }
          }
          </body></html>)
      }

    }

}