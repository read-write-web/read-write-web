package org.w3.readwriteweb

import org.w3.readwriteweb.util.modelFromString

import java.io.{InputStream, StringReader}
import java.net.URL
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
case class PostBinary(in: InputStream) extends Post
case object PostUnknown extends Post

import scalaz._
import Scalaz._

object Post {
  
  val SPARQL = "application/sparql-query"
  val supportContentTypes = Lang.supportContentTypes ++ Image.supportedImages.map(_.contentType) + SPARQL
  val supportedAsString = supportContentTypes mkString ", "

  def supportsContentType(contentTypeHeader: String) = {
    supportContentTypes.contains(cleanHeader(contentTypeHeader))
  }

  def cleanHeader(headerVal: String) = headerVal.split(";")(0).trim
  
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def parse(
      is: InputStream,
      base: URL,
      contentType: String): Post = {
    assert(supportsContentType(contentType))

    val inAsString = {
       val source = Source.fromInputStream(is, "UTF-8")
       source.getLines.mkString("\n")
    }

    def postUpdate =
      try {
        val update: UpdateRequest = UpdateFactory.create(inAsString, base.toString)
        PostUpdate(update).success
      } catch {
        case qpe: QueryParseException => qpe.fail
      }
      
    def postRDF(lang: Lang) =
      modelFromString(inAsString, base, lang) flatMap { model => PostRDF(model).success }
    
    def postQuery =
      try {
        val query = QueryFactory.create(inAsString)
        PostQuery(query).success
      } catch {
        case qe: QueryException => qe.fail
      }

    
    cleanHeader(contentType) match {
      case SPARQL => postUpdate | (postQuery | PostUnknown)
      case RequestLang(lang) => postRDF(lang) | PostUnknown
      case GIF.contentType | JPEG.contentType | PNG.contentType => PostBinary(is)
    }

  }
  
}
