package org.w3.readwriteweb

import org.w3.readwriteweb.util.modelFromString

import java.io.{InputStream, StringReader}
import scala.io.Source
import org.slf4j.{Logger, LoggerFactory}
import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._
import com.hp.hpl.jena.shared.JenaException

sealed trait Post
case class PostUpdate(update: UpdateRequest) extends Post
case class PostRDF(model: Model) extends Post
case class PostQuery(query: Query) extends Post
case object PostUnknown extends Post

import scalaz._
import Scalaz._

object Post {
  
  val SPARQL = "application/sparql-query"
  val supportContentTypes = Lang.supportContentTypes + SPARQL
  val supportedAsString = supportContentTypes mkString ", "

  
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def parse(
      is: InputStream,
      baseURI: String,
      contentType: String): Post = {
    assert(supportContentTypes contains contentType)
    val source = Source.fromInputStream(is, "UTF-8")
    val s = source.getLines.mkString("\n")
    parse(s, baseURI, contentType)
  }
  
  def parse(
      s: String,
      baseURI: String,
      contentType: String): Post = {
    assert(supportContentTypes contains contentType)
    
    val reader = new StringReader(s)
    
    def postUpdate =
      try {
        val update: UpdateRequest = UpdateFactory.create(s, baseURI)
        PostUpdate(update).success
      } catch {
        case qpe: QueryParseException => qpe.fail
      }
      
    def postRDF(lang: Lang) =
      modelFromString(s, baseURI, lang) flatMap { model => PostRDF(model).success }
    
    def postQuery =
      try {
        val query = QueryFactory.create(s)
        PostQuery(query).success
      } catch {
        case qe: QueryException => qe.fail
      }
    
    contentType match {
      case SPARQL => postUpdate | (postQuery | PostUnknown)
      case Lang(lang) => postRDF(lang) | PostUnknown
    }

  }
  
}
