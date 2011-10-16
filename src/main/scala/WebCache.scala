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

package org.w3.readwriteweb

import com.hp.hpl.jena.rdf.model.Model
import java.net.URL
import org.apache.http.MethodNotSupportedException
import org.w3.readwriteweb.util._
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
            case Some(mime) => Lang.apply(mime)
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
