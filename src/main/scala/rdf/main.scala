package org.w3.rdf

import java.io.File
import com.hp.hpl.jena.graph.Graph


object Main {
  
  def main(args: Array[String]) = {
    
    //import org.w3.rdf.jena.JenaModel
    //val rdf = ConcreteRDFModel
    val rdf = org.w3.rdf.jena.JenaModel
    
    val rdfxmlParser = org.w3.rdf.rdfxml.RDFXMLParser(rdf)
    
    val g: rdf.Graph = rdfxmlParser.parse(new File("src/test/resources/card.rdf"))
    
    //val s = org.w3.rdf.turtle.ConcreteTurtleSerializer
    val s = org.w3.rdf.jena.JenaTurtleSerializer
    
    println(s.asString(g, new java.net.URL("http://www.w3.org/People/Berners-Lee/card")))
    
  }
  
  
}