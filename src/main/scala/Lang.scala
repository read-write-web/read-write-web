package org.w3.readwriteweb

import unfiltered.request._

sealed trait Lang {
  
  def contentType = this match {
    case RDFXML => "application/rdf+xml"
    case TURTLE => "text/turtle"
    case N3 => "text/n3"
  }
  
  def jenaLang = this match {
    case RDFXML => "RDF/XML-ABBREV"
    case TURTLE => "TURTLE"
    case N3 => "N3"
  }
  
}

object Lang {
  
  val supportedLanguages = Set(RDFXML, TURTLE, N3)
  val supportContentTypes = supportedLanguages map (_.contentType)
  val supportedAsString = supportContentTypes mkString ", "
  
  val default = RDFXML
  
  def apply(contentType: String): Option[Lang] =
    contentType match {
      case "text/n3" => Some(N3)
      case "text/turtle" => Some(TURTLE)
      case "application/rdf+xml" => Some(RDFXML)
      case _ => None
  }
  
  def unapply(contentType: String): Option[Lang] =
    apply(contentType)

  def apply(req: HttpRequest[_]): Option[Lang] =
    RequestContentType(req) flatMap Lang.apply
    
  def unapply(req: HttpRequest[_]): Option[Lang] =
    apply(req)
    
}

case object RDFXML extends Lang

case object TURTLE extends Lang

case object N3 extends Lang
