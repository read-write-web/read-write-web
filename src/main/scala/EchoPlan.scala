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

import io.BufferedSource
import unfiltered.Cycle
import unfiltered.netty.ReceivedMessage
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import unfiltered.request.{HttpRequest, Path}
import org.jboss.netty.handler.codec.http.HttpResponse
import unfiltered.response.{Ok, PlainTextContent, ResponseString}


/**
 * Useful Echo Server, for debugging
 *
 * @author hjs
 * @created: 19/10/2011
 */
trait EchoPlan[Req,Res] {
    import collection.JavaConversions._

  /**
   * unfiltered is missing a method to get the header names from the request, so this method is required
   * @param req
   * @return
   */
  def headers(req: HttpRequest[Req]): Iterator[String]

  def intent : Cycle.Intent[Req, Res] = {
    case req@Path(path) if path startsWith "/test/http/echo" => {
      Ok ~> PlainTextContent ~> {
        val header = for (headerName <- headers(req);
                          headerValue <- req.headers(headerName)
        ) yield {
          headerName + ": " + headerValue +"\r\n"
        }
        ResponseString(header.mkString+ "\r\n" + new BufferedSource(req.inputStream).mkString)
      }
    }
  }
}

/**
 * this is a trait so that it can be mixed in with different threading policies
 */
trait NettyEchoPlan extends EchoPlan[ReceivedMessage,HttpResponse] {
  import scala.collection.JavaConverters.collectionAsScalaIterableConverter

  def headers(req: HttpRequest[ReceivedMessage]) = req.underlying.request.getHeaderNames.asScala.toIterator
}


object JettyEchoPlan extends EchoPlan[HttpServletRequest,HttpServletResponse] {
  import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter
  import java.util.Enumeration
  def headers(req: HttpRequest[HttpServletRequest]) = Option(req.underlying.getHeaderNames).
    map(enum=> enumerationAsScalaIteratorConverter[String](enum.asInstanceOf[Enumeration[String]]).asScala).
    get
}
