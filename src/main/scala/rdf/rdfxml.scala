package org.w3.rdf.rdfxml

//import org.w3.rdf.{Model => RDFModel, Implicits => RDFImplicits, _}
import org.w3.rdf._

import com.hp.hpl.jena.rdf.arp._
import org.xml.sax._
import org.xml.sax.helpers._
import java.io._
import java.net.{MalformedURLException, URL, URLConnection}
import java.util.{StringTokenizer, Enumeration, Hashtable}
import java.net.URI

sealed trait ParseErrorType
object RDFParseError extends ParseErrorType { override def toString:String = "RDF" }
object XMLParseError extends ParseErrorType { override def toString:String = "XML" }

sealed trait ParseErrorLevel
object FatalError extends ParseErrorLevel { override def toString:String = "FatalError" }
object Error extends ParseErrorLevel { override def toString:String = "Error" }
object Warning extends ParseErrorLevel { override def toString:String = "Warning" }

case class ParseError(
  val message:String,
  val errorType:ParseErrorType,
  val level:ParseErrorLevel,
  val column:Int,
  val line:Int)

object ParseError {
  def fromSAXParseException(e:SAXParseException, level:ParseErrorLevel):ParseError = {
    val message = e.getMessage
    val errorType = if (e.isInstanceOf[ParseException]) RDFParseError else XMLParseError
    val correctedLevel =
      if      (message.size > 1 && message(1) == 'W') Warning
      else if (message.size > 1 && message(1) == 'E') Error
      else    level
    new ParseError(message, errorType, correctedLevel, e.getColumnNumber, e.getLineNumber)
  }
}

object RDFXMLParser {
  
  def apply(rdf: RDFModel) = new Object {
    private val parser = new RDFXMLParser
    def parse(file:File): rdf.Graph = parser.toGraph(file)(rdf)._1
  }
  
}

class RDFXMLParser {
  
  /**
   * http://jena.sourceforge.net/javadoc/com/hp/hpl/jena/rdf/arp/AResource.html
   * note: see setUserData and getUserData for when BNode will be abstract
   */
  def toNode(a: AResource)(implicit rdf: RDFModel): rdf.Node =
    if (a.isAnonymous)
      rdf.NodeBNode(rdf.BNode(a.getAnonymousID))
    else
      rdf.NodeIRI(rdf.IRI(a.getURI))
  
  def toPredicate(a: AResource)(implicit rdf: RDFModel): rdf.Predicate =
    rdf.PredicateIRI(rdf.IRI(a.getURI))
  
  def toLiteral(l: ALiteral)(implicit rdf: RDFModel): rdf.Literal = {
    val datatype:String = l.getDatatypeURI
    if (datatype == null) {
      val lang = l.getLang match {
        case "" => None
        case l  => Some(rdf.LangTag(l))
      }
      rdf.PlainLiteral(l.toString, lang)
    } else {
      rdf.TypedLiteral(l.toString, rdf.IRI(datatype))
    }
  }
  
  def toGraph(file:File)(implicit rdf: RDFModel): (rdf.Graph, List[ParseError]) =
    toGraph(new FileInputStream(file))(rdf)
  
  def toGraph(rdfxml:String)(implicit rdf: RDFModel): (rdf.Graph, List[ParseError]) =
    toGraph(new StringReader(rdfxml))(rdf)
  
  def toGraph(in:InputStream)(implicit rdf: RDFModel): (rdf.Graph, List[ParseError]) =
    toGraph(new BufferedReader(new InputStreamReader(in)))(rdf)
  
  def toGraph(in:Reader)(implicit rdf: RDFModel): (rdf.Graph, List[ParseError]) = {
    
    // the accumulator for the triples
    var triples = Set[rdf.Triple]()
    
    // the accumulators for the problems we encounter
    var parseErrors = List[ParseError]()
    
    // this ErrorHandler keeps track of all the problems during the parsing
    val errorHandler = new ErrorHandler {
      def fatalError(e:SAXParseException):Unit = parseErrors ::= ParseError.fromSAXParseException(e, FatalError)
      def error(e:SAXParseException):Unit = parseErrors ::= ParseError.fromSAXParseException(e, Error)
      def warning(e:SAXParseException):Unit = parseErrors ::= ParseError.fromSAXParseException(e, Warning)
    }
    
    // this StatementHandler read the parsed triples
    val statementHandler = new StatementHandler {
      def statement(s:AResource, p:AResource, o:ALiteral):Unit =
        triples += rdf.Triple(rdf.SubjectNode(toNode(s)),
                              toPredicate(p),
                              rdf.ObjectLiteral(toLiteral(o)))
      def statement(s:AResource, p:AResource, o:AResource):Unit =
        triples += rdf.Triple(rdf.SubjectNode(toNode(s)),
                              toPredicate(p),
                              rdf.ObjectNode(toNode(o)))
    }
  
    // http://jena.sourceforge.net/ARP/standalone.html
  
    val arp = new ARP
    arp.getOptions.setStrictErrorMode
    arp.getHandlers.setErrorHandler(errorHandler)
    arp.getHandlers.setStatementHandler(statementHandler)
    arp.load(in)
  
    // returns an immutable set and the potential errors
    (rdf.Graph(triples), parseErrors)
  }
  

}
