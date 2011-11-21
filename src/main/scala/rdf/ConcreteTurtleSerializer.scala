package org.w3.rdf.concrete

import org.w3.rdf.turtle.TurtleSerializer
import java.net.URL

object ConcreteTurtleSerializer extends TurtleSerializer[ConcreteRDFModel] {
  
  val rdf = ConcreteRDFModel
  
  def asString(g: rdf.Graph, base: URL): String = {
    g map {
      t =>
        val rdf.Triple(rdf.SubjectNode(s), rdf.PredicateIRI(p), o) = t
        try {
          "%s %s %s" format (nodeStr(s), iriStr(p), objectStr(o))
        } catch {
          case e => {
            println("=== "+t)
            println("s: "+s)
            println("p: "+p)
            println("o: "+o)
            throw e
          }
        }
    } mkString "\n"
  }

  def objectStr(n: rdf.Object): String = {
    n match {
//       case l:rdf.ObjectLiteral => {
//         val x:rdf.ObjectLiteral = l
//         "**ObjectLiteral(" + x + ")**"
//       }
      case rdf.ObjectNode(n) => nodeStr(n)
      case rdf.ObjectLiteral(l) => literalStr(l)
      case x => { sys.error(x.toString) }
    }
  }

  private def nodeStr(n: rdf.Node): String = {
    n match {
      case rdf.NodeIRI(i) => iriStr(i)
      case rdf.NodeBNode(b) => bnodeStr(b)
    }
  }

  private def iriStr(i: rdf.IRI): String =
    "<%s>" format { val rdf.IRI(s) = i; s }

  private def bnodeStr(b: rdf.BNode): String =
    "_:" + { val rdf.BNode(l) = b; l }
 
  private def literalStr(l: rdf.Literal): String = l.toString

}
