package org.w3.rdf

import java.io.File
import com.hp.hpl.jena.graph.Graph


object Main {
  
  def main(args: Array[String]) = {
    
//    val turtle = new org.w3.rdf.turtle.TurtleParser(org.w3.rdf.jena.JenaModel)
//    
//    val g = turtle.toGraph(new File("/tmp/card.n3"))

    //import org.w3.rdf.jena.JenaModel
    val rdf = ConcreteRDFModel
    
    val rdfxmlParser = org.w3.rdf.rdfxml.RDFXMLParser(rdf)
    
    val g: rdf.Graph = rdfxmlParser.parse(new File("/tmp/card.rdf"))

//    val m:Graph = g.jenaGraph
    
    val s = new org.w3.rdf.turtle.TurtleSerializer
    
    //println(s.showAsString(rdf)(g))
    
    import rdf._
    val ol = ObjectLiteral(PlainLiteral("The Next Wave of the Web (Plenary Panel)",None))
    
    println(s.objectStr(rdf)(ol))
    
    
    
  }
  
  
}