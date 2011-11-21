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

import unfiltered.request.Path
import unfiltered.response.{ResponseString, PlainTextContent, ContentType, Ok}
import io.BufferedSource


/**
 * @author hjs
 * @created: 19/10/2011
 */

class EchoPlan {
  import collection.JavaConversions._

  lazy val plan = unfiltered.filter.Planify({
    case req@Path(path) if path startsWith "/test/http/echo" => {
      Ok ~> PlainTextContent ~> {
        val headers = req.underlying.getHeaderNames()
        val result = for (name <- headers ;
              nameStr = name.asInstanceOf[String]
        ) yield {
          nameStr + ": " + req.underlying.getHeader(nameStr)+"\r\n"
        }
        ResponseString(result.mkString+ "\r\n" + new BufferedSource(req.inputStream).mkString)
      }
    }

  })
}