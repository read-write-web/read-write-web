package org.w3.readwriteweb

import unfiltered.request._

sealed trait RDFEncoding {
  def toContentType:String
}

case object RDFXML extends RDFEncoding {
  def toContentType = "application/rdf+xml"
}

case object TURTLE extends RDFEncoding {
  def toContentType = "text/turtle"
}

object RDFEncoding {
  
  def apply(contentType:String):RDFEncoding =
    contentType match {
      case "text/turtle" => TURTLE
      case "application/rdf+xml" => RDFXML
      case _ => RDFXML
    }
    
  def apply(req:HttpRequest[_]):RDFEncoding = {
    val contentType = Accept(req).headOption
    contentType map { RDFEncoding(_) } getOrElse RDFXML
  }
  
}
