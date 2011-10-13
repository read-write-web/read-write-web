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