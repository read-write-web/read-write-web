package org.w3.readwriteweb

import java.io._
import scala.io.Source

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._
import com.hp.hpl.jena.shared.JenaException

import org.w3.readwriteweb.util._

sealed trait Post
case class PostUpdate(update:UpdateRequest) extends Post
case class PostRDF(model:Model) extends Post

object Post {
  
  def parse(is:InputStream, baseURI:String):Post = {
    val source = Source.fromInputStream(is, "UTF-8")
    val s = source.getLines.mkString("\n")
    parse(s, baseURI)
  }
  
  def parse(s:String, baseURI:String):Post = {
    val reader = new StringReader(s)
    try {
      val update:UpdateRequest = UpdateFactory.create(s, baseURI)
      PostUpdate(update)      
    } catch {
      case qpe:QueryParseException => {
        val model = modelFromString(s, baseURI)
        PostRDF(model)
      }
    }
  }
  
}
