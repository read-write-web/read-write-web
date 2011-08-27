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

package object util {
  
  val defaultLang = "RDF/XML-ABBREV"

  class MSAuthorVia(value:String) extends ResponseHeader("MS-Author-Via", List(value))
  object ViaSPARQL extends MSAuthorVia("SPARQL")
  
  object ResponseModel {
    def apply(model:Model, base:String, lang:String = defaultLang):ResponseStreamer =
      new ResponseStreamer {
        def stream(os:OutputStream):Unit = model.write(os, lang, base)
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