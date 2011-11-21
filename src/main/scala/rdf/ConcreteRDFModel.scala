package org.w3.rdf

import org.w3.isomorphic._

trait ConcreteRDFModel extends RDFModel {

  case class IRI(iri: String) { override def toString = '"' + iri + '"' }
  object IRI extends Isomorphic1[String, IRI]

  case class Graph(triples: Set[Triple]) extends GraphLike {
    def iterator = triples.iterator
    def ++(other: Graph): Graph = Graph(triples ++ other.triples)
  }
  object Graph {
    def empty: Graph = Graph(Set[Triple]())
    def apply(elems: Triple*):Graph = Graph(Set[Triple](elems: _*))
    def apply(it: Iterable[Triple]): Graph = Graph(it.toSet)
  }

  case class Triple(s: Subject, p: Predicate, o: Object)
  object Triple extends Isomorphic3[Subject, Predicate, Object, Triple]

  case class BNode(label: String)
  object BNode extends Isomorphic1[String, BNode]

  sealed trait Node
  case class NodeIRI(i: IRI) extends Node
  object NodeIRI extends Isomorphic1[IRI, NodeIRI]
  case class NodeBNode(b: BNode) extends Node
  object NodeBNode extends Isomorphic1[BNode, NodeBNode]

  sealed trait Subject
  case class SubjectNode(n: Node) extends Subject
  object SubjectNode extends Isomorphic1[Node, SubjectNode]

  sealed trait Predicate
  case class PredicateIRI(i: IRI) extends Predicate
  object PredicateIRI extends Isomorphic1[IRI, PredicateIRI]

  sealed trait Object {
//    def unapply(o: Object): Option[Object] = {
//      println("*** do I get here?")
//      o match {
//        case on: ObjectNode => Some(on)
//        case ol: ObjectLiteral => Some(ol)
//        case _ => None
//      }
//    } 
  }
  case class ObjectNode(n: Node) extends Object
  object ObjectNode extends Isomorphic1[Node, ObjectNode]
  case class ObjectLiteral(n: Literal) extends Object
  object ObjectLiteral extends Isomorphic1[Literal, ObjectLiteral]
  
  sealed abstract class Literal(val lexicalForm: String)
  object Literal extends PatternMatching1[Literal, Literal] {
    def unapply(l: Literal): Option[Literal] ={
      println("&&& do I get here?")
      l match {
        case pl@PlainLiteral(lexicalForm, langtag) => Some(pl)
        case tl@TypedLiteral(lexicalForm, datatype) => Some(tl)
        case _ => None
      }
    } 
  }
  case class PlainLiteral(override val lexicalForm: String, langtag: Option[LangTag]) extends Literal(lexicalForm) {
    def lang = langtag map { "@" + _ } getOrElse ""
    //override def toString = "\"%s\"%s" format (lexicalForm, lang)
  }
  object PlainLiteral extends Isomorphic2[String, Option[LangTag], PlainLiteral]
  case class TypedLiteral(override val lexicalForm: String, datatype: IRI) extends Literal(lexicalForm) {
    //override def toString = "\"%s\"^^%s" format (lexicalForm, datatype)
  }
  object TypedLiteral extends Isomorphic2[String, IRI, TypedLiteral]

  case class LangTag(s: String)
  object LangTag extends Isomorphic1[String, LangTag]

}

object ConcreteRDFModel extends ConcreteRDFModel

object ConcreteModelWithImplicits extends ConcreteRDFModel with Implicits