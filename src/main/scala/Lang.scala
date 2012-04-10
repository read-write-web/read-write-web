/*
 * Copyright (c) 2011 World Wide Web Consortium
 * under the W3C licence defined at http://opensource.org/licenses/W3C
 */

package org.w3.readwriteweb

sealed trait Lang {

  def suffix = this match {
    case RDFXML => ".rdf"
    case TURTLE => ".ttl"
    case N3 => ".n3"
    case XHTML => ".xhtml"
    case HTML => ".html"
  }
  
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
      case "application/xhtml+xml" => Some(XHTML)
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