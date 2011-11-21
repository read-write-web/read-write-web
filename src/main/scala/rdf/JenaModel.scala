package org.w3.rdf.jena

import org.w3.rdf._
import com.hp.hpl.jena.graph.{Graph => JenaGraph, Triple => JenaTriple, Node => JenaNode, _}
import com.hp.hpl.jena.rdf.model.{AnonId}
import com.hp.hpl.jena.datatypes.{RDFDatatype, TypeMapper}
import org.w3.readwriteweb.util.trySome

import org.w3.isomorphic._

trait JenaModel extends RDFModel {

  case class IRI(iri: String) { override def toString = '"' + iri + '"' }
  object IRI extends Isomorphic1[String, IRI]

  class Graph(val jenaGraph: JenaGraph) extends GraphLike {
    def iterator: Iterator[Triple] = new Iterator[Triple] {
      val iterator = jenaGraph.find(JenaNode.ANY, JenaNode.ANY, JenaNode.ANY)
      def hasNext = iterator.hasNext
      def next = iterator.next
    }
    
    def ++(other: Graph): Graph = {
      val g = Factory.createDefaultGraph
      iterator foreach { t => g add t }
      other.iterator foreach { t => g add t }
      new Graph(g)
    }

    override def equals(o: Any): Boolean =
      ( o.isInstanceOf[Graph] && jenaGraph.isIsomorphicWith(o.asInstanceOf[Graph].jenaGraph) )

  }

  object Graph {
    def empty: Graph = new Graph(Factory.createDefaultGraph)
    def apply(elems: Triple*): Graph = apply(elems.toIterable)
    def apply(it: Iterable[Triple]): Graph = {
      val jenaGraph = Factory.createDefaultGraph
      it foreach { t => jenaGraph add t }
      new Graph(jenaGraph)
    }
  }

  type Triple = JenaTriple
  object Triple extends Isomorphic3[Subject, Predicate, Object, Triple] {
    def apply(s: Subject, p: Predicate, o: Object): Triple = JenaTriple.create(s, p, o)
    def unapply(t: Triple): Option[(Subject, Predicate, Object)] =
      Some((t.getSubject, t.getPredicate, t.getObject))
  }

  type BNode = Node_Blank
  object BNode extends Isomorphic1[String, BNode] {
    def apply(label: String): BNode = {
      val id = AnonId.create(label)
      JenaNode.createAnon(id).asInstanceOf[Node_Blank]
    }
    def unapply(bn: BNode): Option[String] = trySome(bn.getBlankNodeId.getLabelString)
  }

  type Node = JenaNode
  type NodeIRI = Node_URI
  object NodeIRI extends Isomorphic1[IRI, NodeIRI] {
    def apply(iri: IRI): NodeIRI = {
      val IRI(s) = iri
      JenaNode.createURI(s).asInstanceOf[Node_URI]
    }
    def unapply(node: NodeIRI): Option[IRI] = trySome(IRI(node.getURI))
  }
  type NodeBNode = Node_Blank
  object NodeBNode extends Isomorphic1[BNode, NodeBNode] {
    def apply(node: BNode): NodeBNode = node
    def unapply(node: NodeBNode): Option[BNode] =
      if (node.isBlank) Some(node) else None
  }

  type Subject = JenaNode
  type SubjectNode = JenaNode
  object SubjectNode extends Isomorphic1[Node, SubjectNode] {
    def apply(node: Node): SubjectNode = node
    def unapply(node: SubjectNode): Option[Node] = Some(node)
  }

  type Predicate = JenaNode
  type PredicateIRI = JenaNode
  object PredicateIRI extends Isomorphic1[IRI, PredicateIRI] {
    def apply(iri: IRI): PredicateIRI = { val IRI(s) = iri ; JenaNode.createURI(s) }
    def unapply(node: PredicateIRI): Option[IRI] = trySome(IRI(node.getURI))
  }

  type Object = JenaNode
  type ObjectNode = JenaNode
  object ObjectNode extends Isomorphic1[Node, ObjectNode] {
    def apply(node: Node): ObjectNode = node
    def unapply(node: ObjectNode): Option[Node] =
      if (node.isURI || node.isBlank) Some(node) else None
  }
  type ObjectLiteral = JenaNode
  object ObjectLiteral extends Isomorphic1[Literal, ObjectLiteral] {
    def apply(literal: Literal): ObjectLiteral = literal
    def unapply(node: ObjectLiteral): Option[Literal] =
      if (node.isLiteral) Some(node.asInstanceOf[Node_Literal]) else None
  }

  type Literal = Node_Literal
  type PlainLiteral = Node_Literal
  object PlainLiteral extends Isomorphic2[String, Option[LangTag], PlainLiteral] {
    def apply(lit: String, langtagOption: Option[LangTag]) =
      langtagOption match {
        case Some(LangTag(langtag)) => JenaNode.createLiteral(lit, langtag, false).asInstanceOf[Node_Literal]
        case None => JenaNode.createLiteral(lit).asInstanceOf[Node_Literal]
      }
    def unapply(literal: PlainLiteral): Option[(String, Option[LangTag])] =
      trySome { ( literal.getLiteralValue.toString, Option(LangTag(literal.getLiteralLanguage)) ) }
  }
  
  type TypedLiteral = Node_Literal
  lazy val mapper = TypeMapper.getInstance
  object TypedLiteral extends Isomorphic2[String, IRI, TypedLiteral] {
    def apply(lit: String, iri: IRI): TypedLiteral = {
      val IRI(typ) = iri
      JenaNode.createLiteral(lit, null, mapper.getTypeByName(typ)).asInstanceOf[Node_Literal]
    }
    def unapply(literal: TypedLiteral): Option[(String, IRI)] =
      trySome((literal.getLiteralValue.toString, IRI(literal.getLiteralDatatype.getURI)))
  }

  case class LangTag(s: String)
  object LangTag extends Isomorphic1[String, LangTag]

}

object JenaModel extends JenaModel

object JenaModelWithImplicits extends JenaModel with Implicits