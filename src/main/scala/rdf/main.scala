package org.w3.rdf

import java.io.File
import com.hp.hpl.jena.graph.Graph




trait ResourceManager {
  type Resource <: BasicResource
  trait BasicResource {
    def hash : String
    def duplicates(r : Resource) : Boolean
  }
  def create : Resource

//  // Test methods: exercise is to move them outside ResourceManager
//  def testHash(r : Resource) = assert(r.hash == "9e47088d")  
//  def testDuplicates(r : Resource) = assert(r.duplicates(r))
}

trait FileManager extends ResourceManager {
  type Resource <: File
  trait File extends BasicResource {
    def local : Boolean
  }
  override def create : Resource
}

class NetworkFileManager extends FileManager {
  type Resource = RemoteFile
  class RemoteFile extends File {
    def local = false
    def hash = "9e47088d"
    def duplicates(r : Resource) = (local == r.local) && (hash == r.hash)
  }
  override def create : Resource = new RemoteFile
}




object Main {
  
  def main(args: Array[String]) = {
    
//    val turtle = new org.w3.rdf.turtle.TurtleParser(org.w3.rdf.jena.JenaModel)
//    
//    val g = turtle.toGraph(new File("/tmp/card.n3"))

    import org.w3.rdf.jena.JenaModel
    
    val rdfxmlParser = org.w3.rdf.rdfxml.RDFXMLParser(JenaModel)
    
    val g: JenaModel.Graph = rdfxmlParser.parse(new File("/tmp/card.rdf"))

//    val m:Graph = g.jenaGraph
    
    println(g)
    
    val nfm = new NetworkFileManager
    val rf : nfm.Resource = nfm.create
//    nfm.testHash(rf)
//    nfm.testDuplicates(rf)
//
//    def testHash4(rm : ResourceManager)(r : rm.Resource) = 
//      assert(r.hash == "9e47088d")
//
//    def testDuplicates4(rm : ResourceManager)(r : rm.Resource) = 
//      assert(r.duplicates(r))

      
      
    
  }
  
  
}