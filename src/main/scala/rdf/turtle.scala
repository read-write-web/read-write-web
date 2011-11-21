package org.w3.rdf.turtle

import org.w3.rdf._
import scala.util.parsing.combinator._
import java.net.URI

object MyParsers extends RegexParsers {
  val uri = """[a-zA-Z0-9:/#_\.\-\+]+""".r
  val integer = """[0-9]+""".r
  val float = """([0-9]+)?\.([0-9]+)?""".r
  val name = """[a-zA-Z][a-zA-Z0-9_-]*|[a-zA-Z_][a-zA-Z0-9_]+""".r
  var prefixes:Map[String, String] = Map()
  var nextBNode = 1
}

import MyParsers._

class TurtleParser extends JavaTokenParsers {
  
  import rdf._
  
  def toGraph(t:String)(implicit rdf: RDFModel): rdf.Graph = parseAll(turtle, t).get

  def toGraph(file:java.io.File)(implicit rdf: RDFModel): rdf.Graph = {
    val t = scala.io.Source.fromFile(file).getLines.reduceLeft(_+_)
    parseAll(turtle, t).get
  }
 
  def turtle(implicit rdf: RDFModel): Parser[rdf.Graph] =
    opt(triplesblock) ^^ { case tbOPT => tbOPT.getOrElse(rdf.Graph.empty) }
    
  def prefixdecl(implicit rdf: RDFModel): Parser[Unit] =
    "@prefix" ~ name ~ ":" ~ qnameORuri ~ "." ^^ { case "@prefix"~pre~":"~u~"." => prefixes += (pre -> { val rdf.IRI(i: String) = u ; i }) }
    
  def triplesblock(implicit rdf: RDFModel): Parser[rdf.Graph] =
    rep(triplepatternOrPrefixOrBase) ^^ {
      case pats => rdf.Graph(pats.flatten(_.toTraversable))
    }
    
    def triplepatternOrPrefixOrBase(implicit rdf: RDFModel): Parser[Option[rdf.Triple]] = (
        triplepattern ^^ { case p => Some(p) }
      | prefixdecl ^^ { case _ => None }
    )
      
    def triplepattern(implicit rdf: RDFModel): Parser[rdf.Triple] =
      subject ~ predicate ~ objectt ~ "." ^^ { case s~p~o~"." => rdf.Triple(s, p, o) }
  
    def bnode(implicit rdf: RDFModel): Parser[rdf.BNode] =
      "_:"~name ^^ { case "_:"~name => rdf.BNode(name) }

    def literal(implicit rdf: RDFModel): Parser[rdf.Literal] = (
        stringLiteral~"^^"~qnameORuri ^^ {
        case lit~"^^"~dt => rdf.TypedLiteral(lit.substring(1,lit.size - 1), dt match {
          case rdf.IRI("http://www.w3.org/2001/XMLSchema#string") => rdf.StringDatatype
          case rdf.IRI("http://www.w3.org/2001/XMLSchema#integer") => rdf.IntegerDatatype
          case rdf.IRI("http://www.w3.org/2001/XMLSchema#float") => rdf.FloatDatatype
          case rdf.IRI("http://www.w3.org/2001/XMLSchema#date") => rdf.DateDatatype
          case rdf.IRI("http://www.w3.org/2001/XMLSchema#dateTime") => rdf.DateTimeDatatype
          case x => sys.error("only programed to deal with string and integer, not " + x)
        })
      } |
        stringLiteral ^^ {
        case lit => rdf.PlainLiteral(lit.substring(1,lit.size - 1), None)
      } |
        float ^^ { l => rdf.TypedLiteral(l, rdf.FloatDatatype) } |
        integer ^^ { l => rdf.TypedLiteral(l, rdf.IntegerDatatype) }
    )

    def subject(implicit rdf: RDFModel): Parser[rdf.Subject] = {
        qnameORuri ^^ { case x => rdf.SubjectNode(rdf.NodeIRI(x)) } |
        bnode ^^ { case x => rdf.SubjectNode(rdf.NodeBNode(x)) }
    }
      
    def predicate(implicit rdf: RDFModel): Parser[rdf.Predicate] = (
        qnameORuri ^^ { case x => rdf.PredicateIRI(x) }
      | "a" ^^ { x => rdf.PredicateIRI(rdf.IRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) }
    )
      
    def objectt(implicit rdf: RDFModel): Parser[rdf.Object] = {
        qnameORuri ^^ { case x => rdf.ObjectNode(rdf.NodeIRI(x)) } |
        bnode ^^ { case x => rdf.ObjectNode(rdf.NodeBNode(x)) } |
        literal ^^ { case x => rdf.ObjectLiteral(x) }
    }
  
    def qnameORuri(implicit rdf: RDFModel): Parser[rdf.IRI] = (
        "<"~uri~">" ^^ { case "<"~x~">" => rdf.IRI(x) } |
        name~":"~name ^^ {
        case prefix~":"~localName => try {
          rdf.IRI(prefixes(prefix) + localName)
        } catch {
          case e:java.util.NoSuchElementException =>
            throw new Exception("unknown prefix " + prefix)
        }
      }
    )
  
}

// as the Module type does not escape from any method here, we can pass it at the constructor level
class TurtleSerializer {
  
  //type RDFModel = ConcreteRDFModel
  
  class Def[C](implicit desired : Manifest[C]) {
     def unapply[X](c : X)(implicit m : Manifest[X]) : Option[C] = {
       def sameArgs = desired.typeArguments.zip(m.typeArguments).forall {case (desired,actual) => desired >:> actual}
       if (desired >:> m && sameArgs) Some(c.asInstanceOf[C])
       else None
     }
   }
  
  def showAsString(rdf: RDFModel)(g: rdf.Graph): String = {
    g map {
      t =>
        //val rdf.Triple(rdf.SubjectNode(s), rdf.PredicateIRI(p), o: rdf.Object) = t
        val rdf.Triple(rdf.SubjectNode(s), rdf.PredicateIRI(p), o: rdf.Object) = t
        try {
          "%s %s %s" format (nodeStr(rdf)(s), iriStr(rdf)(p), objectStr(rdf)(o))
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

  def objectStr(rdf: RDFModel)(n: rdf.Object): String = {
    n match {
      case rdf.ObjectNode(on) => nodeStr(rdf)(on)
      case rdf.ObjectLiteral(l) => literalStr(rdf)(l)
//      case on: rdf.ObjectNode => { val rdf.ObjectNode(oo) = on; nodeStr(rdf)(oo) }
//      case l: rdf.ObjectLiteral => { val rdf.ObjectLiteral(ll) = l; literalStr(rdf)(ll) }
      
      case x => { sys.error(x.toString) }
    }
  }

  private def nodeStr(rdf: RDFModel)(n: rdf.Node): String = {
    n match {
      case rdf.NodeIRI(i) => iriStr(rdf)(i)
      case rdf.NodeBNode(b) => bnodeStr(rdf)(b)
    }
  }

  private def iriStr(rdf: RDFModel)(i: rdf.IRI): String =
    "<%s>" format { val rdf.IRI(s) = i; s }

  private def bnodeStr(rdf: RDFModel)(b: rdf.BNode): String =
    "_:" + { val rdf.BNode(l) = b; l }
 
  private def literalStr(rdf: RDFModel)(l: rdf.Literal): String = l.toString

}

