package org.w3.rdf

import org.w3.isomorphic._

trait RDFModel { self =>

  type IRI
  trait GraphLike extends Iterable[Triple] { self =>
    def ++(other: Graph): Graph
  }
  type Graph <: GraphLike {
    def ++(other: Graph): Graph
  }
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

  val Literal: PatternMatching1[Literal, Literal]
  val PlainLiteral: Isomorphic2[String, Option[LangTag], PlainLiteral]
  val TypedLiteral: Isomorphic2[String, IRI, TypedLiteral]

  val LangTag: Isomorphic1[String, LangTag]

  lazy val StringDatatype = IRI("http://www.w3.org/2001/XMLSchema#string")
  lazy val IntegerDatatype = IRI("http://www.w3.org/2001/XMLSchema#integer")
  lazy val FloatDatatype = IRI("http://www.w3.org/2001/XMLSchema#float")
  lazy val DateDatatype = IRI("http://www.w3.org/2001/XMLSchema#date")
  lazy val DateTimeDatatype = IRI("http://www.w3.org/2001/XMLSchema#dateTime")

}


trait Implicits extends RDFModel {
  implicit def iri2nodeiri(i: IRI): Node = NodeIRI(i)
  implicit def bnode2nodebnode(b: BNode): Node = NodeBNode(b)
  implicit def node2subjectnode(n: Node): Subject = SubjectNode(n)
  implicit def iri2subjectnode(i: IRI): Subject = SubjectNode(i)
  implicit def bnode2subjectnode(b: BNode): Subject = SubjectNode(b)
  implicit def iri2predicateiri(i: IRI): Predicate = PredicateIRI(i)
  implicit def node2objectnode(n: Node): Object = ObjectNode(n)
  implicit def iri2objectnode(i: IRI): Object = ObjectNode(i)
  implicit def bnode2objectnode(b: BNode): Object = ObjectNode(b)
  implicit def typed2object(i: TypedLiteral): Object = ObjectLiteral(i)
  implicit def plain2object(b: PlainLiteral): Object = ObjectLiteral(b)
}

