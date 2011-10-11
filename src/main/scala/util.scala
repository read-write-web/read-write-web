package org.w3.readwriteweb

import javax.servlet._
import javax.servlet.http._
import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

import java.io._
import scala.io.Source

import scalaz._
import Scalaz._

import _root_.scala.sys.error

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

sealed trait RDFEncoding {
  def toContentType:String
}
case object RDFXML extends RDFEncoding {
  def toContentType = "application/rdf+xml"
}
case object TURTLE extends RDFEncoding {
  def toContentType = "text/turtle"
}

object RDFEncoding {
  
  def apply(contentType:String):RDFEncoding = {
    val i = contentType.indexOf(';')
    (if (i<0) contentType
    else contentType.substring(0,i).trim()).toLowerCase match {
      case "text/turtle" => TURTLE
      case "application/rdf+xml" => RDFXML
      case _ => RDFXML       
    }
  }

  def jena(encoding: RDFEncoding) = encoding match {
    case RDFXML => "RDF/XML-ABBREV"
    case TURTLE => "TURTLE"
    case _      => "RDF/XML-ABBREV" //don't like this default
  }
    
  def apply(req:HttpRequest[_]):RDFEncoding = {
    val contentType = Accept(req).headOption
    contentType map { RDFEncoding(_) } getOrElse RDFXML
  }
  
}

trait ValidationW[E, S] {
  val validation:Validation[E, S]
  def failMap[EE](f:E => EE):Validation[EE, S] = validation.fail map f validation
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

  def modelFromInputStream(is:InputStream, base: String,  lang: RDFEncoding): Validation[Throwable, Model]=
    modelFromInputStream(is, base, RDFEncoding.jena(lang))
  
  def modelFromInputStream(
      is:InputStream,
      base:String,
      lang:String = "RDF/XML-ABBREV"):Validation[Throwable, Model] =
    try {
      val m = ModelFactory.createDefaultModel()
      m.read(is, base, lang)
      m.success
    } catch {
      case t => t.fail
    }
  
  def modelFromString(s:String,
      base:String,
      lang:String = "RDF/XML-ABBREV"):Validation[Throwable, Model] =
    try {
      val reader = new StringReader(s)
      val m = ModelFactory.createDefaultModel()
      m.read(reader, base, lang)
      m.success
    } catch {
      case t => t.fail
    }

  implicit def wrapValidation[E, S](v:Validation[E,S]):ValidationW[E, S] =
    new ValidationW[E, S] { val validation = v }
  
  implicit def unwrap[E, F <: E, S <: E](v:Validation[F,S]):E = v.fold(e => e, s => s)
  
}


import java.io.{File, FileWriter}
import java.util.jar._
import scala.collection.JavaConversions._
import scala.io.Source
import java.net.{URL, URLDecoder}
import org.slf4j.{Logger, LoggerFactory}

/** useful stuff to read resources from the classpath */
object MyResourceManager {
  
  val logger:Logger = LoggerFactory.getLogger(this.getClass)

  val clazz:Class[_] = this.getClass
  val classloader = this.getClass.getClassLoader
  
  /** http://www.uofr.net/~greg/java/get-resource-listing.html
   */
  def getResourceListing(path:String):List[String] = {
    var dirURL:URL = classloader.getResource(path)
    if (dirURL != null && dirURL.getProtocol == "file") {
      /* A file path: easy enough */
      new File(dirURL.toURI).list.toList
    } else {
      if (dirURL == null) {
        val me = clazz.getName().replace(".", "/")+".class"
        dirURL = classloader.getResource(me)
      }
      if (dirURL.getProtocol == "jar") {
        val jarPath = dirURL.getPath.substring(5, dirURL.getPath().indexOf("!"))
        val jar:JarFile = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))
        val entries = jar.entries filter { _.getName startsWith path } map { e => {
          var entry = e.getName substring path.length
          val checkSubdir = entry indexOf "/"
          if (checkSubdir >= 0) entry = entry.substring(0, checkSubdir)
          entry
        } }
        entries filterNot { _.isEmpty } toList
      } else
        error("Cannot list files for URL "+dirURL);
    }
  }
  
  /** extract a path found in the classpath
   * 
   *  @return the file on disk
   */
  def fromClasspath(path:String, base:File = new File("src/main/resources")):File = {
    val workingPath = new File(base, path)
    if (workingPath isDirectory) {
      workingPath
    } else {
      val dir = new File(System.getProperty("java.io.tmpdir"),
                         "virtual-trainer-" + scala.util.Random.nextInt(10000).toString)
      if (! dir.mkdir()) logger.error("Couldn't extract %s from jar to %s" format (path, dir.getAbsolutePath))
      val entries = getResourceListing(path) foreach { entry => {
        val url = classloader.getResource(path + entry)
        val content = Source.fromURL(url, "UTF-8").getLines.mkString("\n")
        val writer = new FileWriter(new File(dir, entry))
        writer.write(content)
        writer.close()
      }
    }
    dir
    }
  }
  
}
