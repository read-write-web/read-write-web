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

import java.security.Principal

/**
 * @author hjs
 * @created: 13/10/2011
 */

/**
 * @author Henry Story from http://bblfish.net/
 * @created: 09/10/2011
 */

case class WebIdPrincipal(webid: String) extends Principal {
  def getName = webid
  override def equals(that: Any) = that match {
    case other: WebIdPrincipal => other.webid == webid
    case _ => false
  }
}

case class Anonymous() extends Principal {
  def getName = "anonymous"
  override def equals(that: Any) =  that match {
      case other: WebIdPrincipal => other eq this 
      case _ => false
    } //anonymous principals are equal only when they are identical. is this wise?
      //well we don't know when two anonymous people are the same or different.
}