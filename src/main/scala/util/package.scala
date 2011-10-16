package org.w3.readwriteweb

import java.io._
import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import unfiltered.response._
import scalaz._
import Scalaz._

package object util {
  
  val defaultLang = "RDF/XML-ABBREV"

  class MSAuthorVia(value: String) extends ResponseHeader("MS-Author-Via", List(value))
  
  object ViaSPARQL extends MSAuthorVia("SPARQL")
  
  object ResponseModel {
    def apply(model: Model, base: String, lang: Lang): ResponseStreamer =
      new ResponseStreamer {
        def stream(os: OutputStream): Unit =
          model.getWriter(lang.jenaLang).write(model, os, base)
      }
  }

  object ResponseResultSet {
    def apply(rs: ResultSet): ResponseStreamer =
      new ResponseStreamer {
        def stream(os: OutputStream): Unit = ResultSetFormatter.outputAsXML(os, rs) 
      }
    def apply(result: Boolean): ResponseStreamer =
      new ResponseStreamer {
        def stream(os: OutputStream):Unit = ResultSetFormatter.outputAsXML(os, result) 
      }
  }

  //Passing strings into method arguments, especially as these differ completely between rdf stacks is not so good
  //passing objects is better
  def modelFromInputStream(is:InputStream, base: String,  lang: Lang): Validation[Throwable, Model]=
      modelFromInputStream(is, base, lang.jenaLang)

  def modelFromInputStream(
      is: InputStream,
      base: String,
      lang: String = "RDF/XML-ABBREV"): Validation[Throwable, Model] =
    try {
      val m = ModelFactory.createDefaultModel()
      m.read(is, base, lang)
      m.success
    } catch {
      case t => t.fail
    }
  
  def modelFromString(
      s: String,
      base: String,
      lang: String = "RDF/XML-ABBREV"): Validation[Throwable, Model] =
    try {
      val reader = new StringReader(s)
      val m = ModelFactory.createDefaultModel()
      m.read(reader, base, lang)
      m.success
    } catch {
      case t => t.fail
    }

  implicit def wrapValidation[E, S](v: Validation[E,S]): ValidationW[E, S] =
    new ValidationW[E, S] { val validation = v }
  
  implicit def unwrap[E, F <: E, S <: E](v: Validation[F,S]): E = v.fold(e => e, s => s)
  
}
