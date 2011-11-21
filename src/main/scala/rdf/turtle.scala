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
  
  def toGraph(t:String)(implicit rdf: Model): rdf.Graph = parseAll(turtle, t).get

  def toGraph(file:java.io.File)(implicit rdf: Model): rdf.Graph = {
    val t = scala.io.Source.fromFile(file).getLines.reduceLeft(_+_)
    parseAll(turtle, t).get
  }
 
  def turtle(implicit rdf: Model): Parser[rdf.Graph] =
    opt(triplesblock) ^^ { case tbOPT => tbOPT.getOrElse(rdf.Graph.empty) }
    
  def prefixdecl(implicit rdf: Model): Parser[Unit] =
    "@prefix" ~ name ~ ":" ~ qnameORuri ~ "." ^^ { case "@prefix"~pre~":"~u~"." => prefixes += (pre -> { val rdf.IRI(i: String) = u ; i }) }
    
  def triplesblock(implicit rdf: Model): Parser[rdf.Graph] =
    rep(triplepatternOrPrefixOrBase) ^^ {
      case pats => rdf.Graph(pats.flatten(_.toTraversable))
    }
    
    def triplepatternOrPrefixOrBase(implicit rdf: Model): Parser[Option[rdf.Triple]] = (
        triplepattern ^^ { case p => Some(p) }
      | prefixdecl ^^ { case _ => None }
    )
      
    def triplepattern(implicit rdf: Model): Parser[rdf.Triple] =
      subject ~ predicate ~ objectt ~ "." ^^ { case s~p~o~"." => rdf.Triple(s, p, o) }
  
    def bnode(implicit rdf: Model): Parser[rdf.BNode] =
      "_:"~name ^^ { case "_:"~name => rdf.BNode(name) }

    def literal(implicit rdf: Model): Parser[rdf.Literal] = (
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

    def subject(implicit rdf: Model): Parser[rdf.Subject] = {
        qnameORuri ^^ { case x => rdf.SubjectNode(rdf.NodeIRI(x)) } |
        bnode ^^ { case x => rdf.SubjectNode(rdf.NodeBNode(x)) }
    }
      
    def predicate(implicit rdf: Model): Parser[rdf.Predicate] = (
        qnameORuri ^^ { case x => rdf.PredicateIRI(x) }
      | "a" ^^ { x => rdf.PredicateIRI(rdf.IRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) }
    )
      
    def objectt(implicit rdf: Model): Parser[rdf.Object] = {
        qnameORuri ^^ { case x => rdf.ObjectNode(rdf.NodeIRI(x)) } |
        bnode ^^ { case x => rdf.ObjectNode(rdf.NodeBNode(x)) } |
        literal ^^ { case x => rdf.ObjectLiteral(x) }
    }
  
    def qnameORuri(implicit rdf: Model): Parser[rdf.IRI] = (
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
class TurtleSerializer(rdf: Model) {
  
  import rdf._
  
  def graphString(g: Graph):String = {
    g map {
      t => {
	val Triple(SubjectNode(s), PredicateIRI(p), o) = t
	"%s %s %s" format (nodeStr(s), iriStr(p), objectStr(o))
      }
    } mkString "\n"
  }

  def objectStr(n: Object): String = {
    n match {
      // case l:ObjectLiteral => {
      //   val x:ObjectLiteral = l
      //   "**ObjectLiteral(" + x + ")**"
      // }
      case ObjectNode(n) => nodeStr(n)
      case ObjectLiteral(l) => literalStr(l)
      // case x => x
    }
  }

  def nodeStr(n: Node): String = {
    n match {
      case NodeIRI(i) => iriStr(i)
      case NodeBNode(b) => bnodeStr(b)
    }
  }

  def iriStr(i: IRI): String = "<%s>" format { val IRI(s) = i; s }

  def bnodeStr(b: BNode): String = "_:" + { val BNode(l) = b; l }
 
  def literalStr(l: Literal): String = l.toString

}

