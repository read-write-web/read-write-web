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

import javax.servlet.http.HttpServletRequest
import java.security.cert.X509Certificate
import unfiltered.request.HttpRequest
import java.util.concurrent.TimeUnit
import com.google.common.cache.{CacheLoader, CacheBuilder, Cache}

/**
 * @author Henry Story, with help from Doug Tangren on unfiltered mailing list
 * @created: 14/10/2011
 */

object X509Cert {
  def unapply[T <: HttpServletRequest](r: HttpRequest[T]): Option[Array[X509Certificate]] =
    r.underlying.getAttribute("javax.servlet.request.X509Certificate") match {
      case certs: Array[X509Certificate] => Some(certs)
      case _ => None
    }

}