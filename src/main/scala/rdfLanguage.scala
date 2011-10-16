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
  
  val default = RDFXML
  
  def apply: PartialFunction[String, Lang] = {
    case "text/n3" => N3
    case "text/turtle" => TURTLE
    case "application/rdf+xml" => RDFXML
  }

  def fromRequest(req: HttpRequest[_]): Lang = {
    val contentType = Accept(req).headOption
    contentType map { Lang.apply } getOrElse RDFXML
  }

}

case object RDFXML extends Lang

case object TURTLE extends Lang

case object N3 extends Lang
