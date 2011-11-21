package org.w3.rdf.turtle

import org.w3.rdf.RDFModel
import java.net.URL

trait TurtleSerializer[RDF <: RDFModel] {
  
  val rdf: RDF
  
  def asString(g: rdf.Graph, base: URL): String
  
}
