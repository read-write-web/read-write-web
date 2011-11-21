package org.w3.rdf.jena

import org.w3.rdf.turtle.TurtleSerializer
import org.w3.readwriteweb.TURTLE

import scala.util.parsing.combinator._
import java.net.URL
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory}

object JenaTurtleSerializer extends TurtleSerializer[JenaModel] {
  
  val rdf = JenaModel
  
  def asString(g: rdf.Graph, base: URL): String = {
    val model = ModelFactory.createModelForGraph(g.jenaGraph)
    val writer = model.getWriter(TURTLE.jenaLang)
    val sw = new java.io.StringWriter
    writer.write(model, sw, base.toString)
    sw.toString
  }
  
}
