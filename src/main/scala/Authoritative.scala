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

import unfiltered.request._
import java.security.cert.Certificate
import javax.servlet.http.HttpServletRequest
import unfiltered.netty.ReceivedMessage
import org.eclipse.jetty.util.URIUtil
import java.net.{MalformedURLException, URL}

object Authoritative {
  
  val r = """^(.*)\.(\w{0,4})$""".r

  // all of this would be unnecessary if req.uri would really return the full URI
  // we should try to push for that to be done at unfiltered layer
  def reqURL[T](m: Manifest[T], r: HttpRequest[T]): String = {
    if (m <:< manifest[HttpServletRequest]) reqUrlServlet(r.asInstanceOf[HttpRequest[HttpServletRequest]])
    else if (m <:< manifest[ReceivedMessage]) reqUrlNetty(r.asInstanceOf[HttpRequest[ReceivedMessage]])
    else "" //todo: should perhaps throw an exception here.
  }

  def unapply[T](req: HttpRequest[T]) (implicit m: Manifest[T]) : Option[(URL, Representation)] =  {
    val uri = reqURL(m, req)
    val suffixOpt = uri match {
      case r(_, suffix) => Some(suffix)
      case _ if uri endsWith "/" => Some("/")
      case _ => None
    }
    Some((new URL(uri), Representation(suffixOpt, Accept(req))))
  }


  private def reqUrlServlet[T <: HttpServletRequest](req: HttpRequest[T]): String = {
    req.underlying.getRequestURL.toString
  }

  private def reqUrlNetty[T <: ReceivedMessage](req: HttpRequest[T]): String = {
      try {
        val u = new URL(req.uri)
        new URL(u.getProtocol,u.getHost,u.getPort,u.getPath).toExternalForm
      } catch {
        case e:  MalformedURLException => {
          val url: StringBuffer = new StringBuffer (48)
          val scheme = if (req.isSecure) "https" else "http"
          val hostport = {//we assume there was some checking done earlier, and that we can rely on this
          val host = req.headers ("Host")
          if (host.hasNext) host.next () else "localhost"
          }
          url.append (scheme)
          url.append ("://")
          url.append (hostport)
          url.append(req.uri)
          url.toString
        }
      }
  }

}
