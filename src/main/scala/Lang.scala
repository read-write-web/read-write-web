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

sealed trait Lang {
  
  def contentType = this match {
    case RDFXML => "application/rdf+xml"
    case TURTLE => "text/turtle"
    case N3 => "text/n3"
    case XHTML => "application/xhtml+xml"
    case HTML => "text/html"  
  }
  
  def jenaLang = this match {
    case RDFXML => "RDF/XML-ABBREV"
    case TURTLE => "TURTLE"
    case N3 => "N3"
    case HTML => "HTML"
    case XHTML => "XHTML"
  }

}

object Lang {
  
  val supportedLanguages = Set(RDFXML, TURTLE, N3)
  val supportContentTypes = supportedLanguages map (_.contentType)
  val supportedAsString = supportContentTypes mkString ", "
  
  val default = RDFXML
  
  def apply(contentType: String): Option[Lang] =
    contentType.trim.toLowerCase match {
      case "text/n3" => Some(N3)
      case "text/turtle" => Some(TURTLE)
      case "application/rdf+xml" => Some(RDFXML)
      case "text/html" => Some(HTML)
      case "text/xhtml" => Some(XHTML)
      case _ => None
  }    
  
  def apply(cts: Iterable[String]): Option[Lang] =
    cts map Lang.apply collectFirst { case Some(lang) => lang }
    
}

case object RDFXML extends Lang

case object TURTLE extends Lang

case object N3 extends Lang

case object XHTML extends Lang

case object HTML extends Lang