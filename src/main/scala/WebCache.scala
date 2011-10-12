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

import com.hp.hpl.jena.rdf.model.Model
import java.net.URL
import org.apache.http.MethodNotSupportedException
import org.w3.readwriteweb.RDFEncoding._
import org.w3.readwriteweb.util._
import org.w3.readwriteweb.{RDFEncoding, RDFXML}
import scalaz._
import Scalaz._

/**
 * @author Henry Story
 * @created: 12/10/2011
 *
 * The WebCache currently does not cache
 */
class WebCache extends ResourceManager {
  import dispatch._

  val http = new Http
  
  def basePath = null //should be cache dir?

  def sanityCheck() = true  //cache dire exists? But is this needed for functioning?

  def resource(u : URL) = new org.w3.readwriteweb.Resource {

    def get() = {
      // note we prefer rdf/xml and turtle over html, as html does not always contain rdfa, and we prefer those over n3,
      // as we don't have a full n3 parser. Better would be to have a list of available parsers for whatever rdf framework is
      // installed (some claim to do n3 when they only really do turtle)
      // we can't currently accept */* as we don't have GRDDL implemented
      val request = url(u.toString) <:< Map("Accept"->
        "application/rdf+xml,text/turtle,application/xhtml+xml;q=0.8,text/html;q=0.7,text/n3;q=0.6")

      //we need to tell the model about the content type
      val handler: Handler[Validation[Throwable, Model]] = request.>+>[Validation[Throwable, Model]](res =>  {
        res >:> { headers =>
          val encoding = headers("Content-Type").headOption match {
            case Some(mime) => RDFEncoding(mime)
            case None => RDFXML  // it would be better to try to do a bit of guessing in this case by looking at content
          }
          val loc = headers("Content-Location").headOption match {
            case Some(loc) => new URL(u,loc)
            case None => new URL(u.getProtocol,u.getAuthority,u.getPort,u.getPath)
          }
          res>>{ in=>modelFromInputStream(in,loc.toString,encoding) }

        }
      })
      http(handler)

    }

    def save(model: Model) = { throw new MethodNotSupportedException("not implemented"); null }
  }
}
