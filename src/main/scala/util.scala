package org.w3.readwriteweb

import javax.servlet._
import javax.servlet.http._
import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

import java.io._
import scala.io.Source

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._

import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

sealed trait RWWMode
case object AllResourcesAlreadyExist extends RWWMode
case object ResourcesDontExistByDefault extends RWWMode

sealed trait RDFEncoding
case object RDFXML extends RDFEncoding
case object TURTLE extends RDFEncoding

object RDFEncoding {
  
  def apply(contentType:String):RDFEncoding =
    contentType match {
      case "text/turtle" => TURTLE
      case "application/rdf+xml" => RDFXML
      case _ => RDFXML
    }
    
  def apply(req:HttpRequest[_]):RDFEncoding = {
    val contentType = RequestContentType(req)
    contentType map { RDFEncoding(_) } getOrElse RDFXML
  }
  
}

package object util {
  
  val defaultLang = "RDF/XML-ABBREV"

  class MSAuthorVia(value:String) extends ResponseHeader("MS-Author-Via", List(value))
  object ViaSPARQL extends MSAuthorVia("SPARQL")
  
  object ResponseModel {
    def apply(model:Model, base:String, encoding:RDFEncoding):ResponseStreamer =
      new ResponseStreamer {
        def stream(os:OutputStream):Unit =
          encoding match {
            case RDFXML => model.getWriter("RDF/XML-ABBREV").write(model, os, base)
            case TURTLE => model.getWriter("TURTLE").write(model, os, base)
          }
      }
  }

  object ResponseResultSet {
    def apply(rs:ResultSet):ResponseStreamer =
      new ResponseStreamer {
        def stream(os:OutputStream):Unit = ResultSetFormatter.outputAsXML(os, rs) 
      }
    def apply(result:Boolean):ResponseStreamer =
      new ResponseStreamer {
        def stream(os:OutputStream):Unit = ResultSetFormatter.outputAsXML(os, result) 
      }
  }

  def modelFromInputStream(is:InputStream, base:String, lang:String = "RDF/XML-ABBREV"):Model = {
    val m = ModelFactory.createDefaultModel()
    m.read(is, base, lang)
    m
  }
  
  def modelFromString(s:String, base:String, lang:String = "RDF/XML-ABBREV"):Model = {
    val reader = new StringReader(s)
    val m = ModelFactory.createDefaultModel()
    m.read(reader, base, lang)
    m
  }

}