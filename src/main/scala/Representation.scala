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
import java.net.URL

sealed trait Representation

object Representation {
  
  def fromSuffix(suffix: String): Representation = {
    suffix match {
      case "n3" => RDFRepr(N3)
      case "turtle" | "ttl" => RDFRepr(TURTLE)
      case "rdf" => RDFRepr(RDFXML)
      case "htm" | "html" | "xhtml" => HTMLRepr
      case "/" => DirectoryRepr
      case _ => UnknownRepr
    }
  }
  

  val htmlContentTypes = Set("text/html", "application/xhtml+xml")
  
  def acceptsHTML(ct: Iterable[String]) =
    ! (htmlContentTypes & ct.toSet).isEmpty
  
  def fromAcceptedContentTypes(ct: Iterable[String]): Representation = {
    Lang(ct) map RDFRepr.apply getOrElse {
      if (acceptsHTML(ct))
        HTMLRepr
      else
        UnknownRepr
    }
  }
  
  /** implements http://www.w3.org/2001/tag/doc/metaDataInURI-31 and http://www.w3.org/2001/tag/doc/mime-respect
    * 
    * if there is no known suffix (eg. the URI was already the authoritative one),
    * inspects the given accepted content types
    * 
    * This knows only about the RDF and HTML charsets
    */
  def apply(
      suffixOpt: Option[String],
      ct: Iterable[String]): Representation = {
    suffixOpt map fromSuffix match {
      case None | Some(UnknownRepr) => fromAcceptedContentTypes(ct)
      case Some(repr) => repr
    }
  }
}

case class RDFRepr(lang: Lang) extends Representation
case object HTMLRepr extends Representation
case object DirectoryRepr extends Representation
case object UnknownRepr extends Representation
