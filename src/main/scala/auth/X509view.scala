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

import unfiltered.request.Path
import unfiltered.response.{Html, ContentType, Ok}
import org.w3.readwriteweb.WebCache

/**
 * This plan just described the X509 WebID authentication information.
 * This is a simple version. A future version will show EARL output, and so be useful for debugging the endpoint.
 *
 * @author hjs
 * @created: 13/10/2011
 */

class X509view(implicit val webCache: WebCache) {

    val plan = unfiltered.filter.Planify {
      case req @ Path(path) if path startsWith "/test/auth/x509" =>
        Ok ~> ContentType("text/html") ~> Html(
          <html><head><title>Authentication Page</title></head>
        { req match {
          case X509Claim(xclaim: X509Claim) => <body>
            <h1>Authentication Info received</h1>
            <p>You were identified with the following WebIDs</p>
             <ul>{xclaim.webidclaims.filter(cl=>cl.verified).map(p=> <li>{p.webId}</li>)}</ul>
            <p>You sent the following certificate</p>
            <pre>{xclaim.cert.toString}</pre>
          </body>
          case _ => <body><p>We received no Authentication information</p></body>
        }
          }</html>)

      }

}