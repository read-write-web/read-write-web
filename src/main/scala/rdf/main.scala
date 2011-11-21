package org.w3.rdf

import java.io.File
import com.hp.hpl.jena.graph.Graph

// afaik, the compiler doesn't not expose the unapply method
// for a companion object
trait Isomorphic[A, B] {
  def apply(x: A): B
  def unapply(x: B): Option[A]
}

// abstract module
trait Module {
  // 3 types with some contraints
  type X
  type Y <: X
  type Z <: X
  // and their "companion" objects
  def X: Isomorphic[Int, X]
  def Y: Isomorphic[X, Y]
  def Z: Isomorphic[Y, Z]
}

// an implementation relying on case classes
object ConcreteModule extends Module {
  sealed trait X { val i: Int = 42 }
  object X extends Isomorphic[Int, X] {
    def apply(_s: Int): X = new X { }
    def unapply(x: X): Option[Int] = Some(x.i)
  }
  case class Y(x: X) extends X
  // I guess the compiler could do that for me
  object Y extends Isomorphic[X, Y]
  case class Z(y: Y) extends X
  object Z extends Isomorphic[Y, Z]
}

object Main2 {
  def foo(t: Module)(x: t.X): Unit = {
    import t._
    // the output depends on the order of the first 3 lines
    // I'm not sure what's happening here...
    x match {
      // unchecked since it is eliminated by erasure
      case Y(_y) => println("y "+_y)
      // unchecked since it is eliminated by erasure
      case Z(_z) => println("z "+_z)
      // this one is fine
      case X(_x) => println("x "+_x)
      case xyz => println("xyz "+xyz)
    }
  }
  def bar(t: Module): Unit = {
    import t._
    val x: X = X(42)
    val y: Y = Y(x)
    val z: Z = Z(y)
    foo(t)(x)
    foo(t)(y)
    foo(t)(z)
  }
  def main(args: Array[String]) = {
    // call bar with the concrete module
    bar(ConcreteModule)
  }
}

object Main {
  
  def main(args: Array[String]) = {
    
//    
//    val turtle = new org.w3.rdf.turtle.TurtleParser(org.w3.rdf.jena.JenaModel)
//    
//    val g = turtle.toGraph(new File("/tmp/card.n3"))

    //import org.w3.rdf.jena.JenaModel
    //val rdf = ConcreteRDFModel
    val rdf = org.w3.rdf.jena.JenaModel
    
    val rdfxmlParser = org.w3.rdf.rdfxml.RDFXMLParser(rdf)
    
    val g: rdf.Graph = rdfxmlParser.parse(new File("src/test/resources/card.rdf"))

//    val m:Graph = g.jenaGraph
    
    //val s = org.w3.rdf.turtle.ConcreteTurtleSerializer
    val s = org.w3.rdf.turtle.JenaTurtleSerializer
    
    println(s.asString(g, new java.net.URL("http://www.w3.org/People/Berners-Lee/card")))
    
//    import rdf._
//    val ol = ObjectLiteral(PlainLiteral("The Next Wave of the Web (Plenary Panel)",None))
//    
//    println(s.objectStr(ol))
    
    
    
  }
  
  
}