package org.w3.rdf

import org.w3.isomorphic._

trait Model {

  type IRI
  trait GraphLike extends Iterable[Triple] { self =>
    def ++(other: Graph): Graph
  }
  type Graph <: GraphLike
  type Triple
  type BNode
  type Node
  type NodeIRI <: Node
  type NodeBNode <: Node
  type Subject
  type SubjectNode <: Subject
  type Predicate
  type PredicateIRI <: Predicate
  type Object
  type ObjectNode <: Object
  type ObjectLiteral <: Object
  type Literal
  type PlainLiteral <: Literal
  type TypedLiteral <: Literal
  type LangTag

  val IRI: Isomorphic1[String, IRI]

  val Graph: {
    def empty: Graph
    def apply(elems: Triple*): Graph
    def apply(it: Iterable[Triple]): Graph
  }

  val Triple: Isomorphic3[Subject, Predicate, Object, Triple]

  val BNode: Isomorphic1[String, BNode]

  val NodeIRI: Isomorphic1[IRI, NodeIRI]
  val NodeBNode: Isomorphic1[BNode, NodeBNode]

  val SubjectNode: Isomorphic1[Node, SubjectNode]

  val PredicateIRI: Isomorphic1[IRI, PredicateIRI]

  val ObjectNode: Isomorphic1[Node, ObjectNode]
  val ObjectLiteral: Isomorphic1[Literal, ObjectLiteral]

  val PlainLiteral: Isomorphic2[String, Option[LangTag], PlainLiteral]
  val TypedLiteral: Isomorphic2[String, IRI, TypedLiteral]

  val LangTag: Isomorphic1[String, LangTag]

  lazy val StringDatatype = IRI("http://www.w3.org/2001/XMLSchema#string")
  lazy val IntegerDatatype = IRI("http://www.w3.org/2001/XMLSchema#integer")
  lazy val FloatDatatype = IRI("http://www.w3.org/2001/XMLSchema#float")
  lazy val DateDatatype = IRI("http://www.w3.org/2001/XMLSchema#date")
  lazy val DateTimeDatatype = IRI("http://www.w3.org/2001/XMLSchema#dateTime")

}


trait Implicits extends Model {
  implicit def iri2nodeiri(i:IRI):Node = NodeIRI(i)
  implicit def bnode2nodebnode(b:BNode):Node = NodeBNode(b)
  implicit def node2subjectnode(n:Node):Subject = SubjectNode(n)
  implicit def iri2subjectnode(i:IRI):Subject = SubjectNode(i)
  implicit def bnode2subjectnode(b:BNode):Subject = SubjectNode(b)
  implicit def iri2predicateiri(i:IRI):Predicate = PredicateIRI(i)
  implicit def node2objectnode(n:Node):Object = ObjectNode(n)
  implicit def iri2objectnode(i:IRI):Object = ObjectNode(i)
  implicit def bnode2objectnode(b:BNode):Object = ObjectNode(b)
  implicit def typed2object(i:TypedLiteral):Object = ObjectLiteral(i)
  implicit def plain2object(b:PlainLiteral):Object = ObjectLiteral(b)
}

trait ConcreteModel extends Model {

  case class IRI(iri: String) { override def toString = '"' + iri + '"' }
  object IRI extends Isomorphic1[String, IRI]

  case class Graph(triples:Set[Triple]) extends GraphLike {
    def iterator = triples.iterator
    def ++(other:Graph):Graph = Graph(triples ++ other.triples)
  }
  object Graph {
    def empty:Graph = Graph(Set[Triple]())
    def apply(elems:Triple*):Graph = Graph(Set[Triple](elems:_*))
    def apply(it:Iterable[Triple]):Graph = Graph(it.toSet)
  }

  case class Triple (s:Subject, p:Predicate, o:Object)
  object Triple extends Isomorphic3[Subject, Predicate, Object, Triple]

  case class BNode(label:String)
  object BNode extends Isomorphic1[String, BNode]

  sealed trait Node
  case class NodeIRI(i:IRI) extends Node
  object NodeIRI extends Isomorphic1[IRI, NodeIRI]
  case class NodeBNode(b:BNode) extends Node
  object NodeBNode extends Isomorphic1[BNode, NodeBNode]

  sealed trait Subject
  case class SubjectNode(n:Node) extends Subject
  object SubjectNode extends Isomorphic1[Node, SubjectNode]

  sealed trait Predicate
  case class PredicateIRI(i:IRI) extends Predicate
  object PredicateIRI extends Isomorphic1[IRI, PredicateIRI]

  sealed trait Object
  case class ObjectNode(n:Node) extends Object
  object ObjectNode extends Isomorphic1[Node, ObjectNode]
  case class ObjectLiteral (n:Literal) extends Object
  object ObjectLiteral extends Isomorphic1[Literal, ObjectLiteral]

  sealed abstract class Literal(val lexicalForm:String)
  case class PlainLiteral(override val lexicalForm:String, langtag:Option[LangTag]) extends Literal(lexicalForm) {
    override def toString = "\"" + lexicalForm + "\"" + { if (langtag.isDefined) langtag.get }
  }
  object PlainLiteral extends Isomorphic2[String, Option[LangTag], PlainLiteral]
  case class TypedLiteral(override val lexicalForm:String, datatype:IRI) extends Literal(lexicalForm) {
    override def toString = "\"" + lexicalForm + "\"^^" + datatype
  }
  object TypedLiteral extends Isomorphic2[String, IRI, TypedLiteral]

  case class LangTag(s:String)
  object LangTag extends Isomorphic1[String, LangTag]

}

object ConcreteModel extends ConcreteModel

object ConcreteModelWithImplicits extends ConcreteModel with Implicits