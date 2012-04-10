package org.w3.readwriteweb.util

import org.w3.readwriteweb._

import auth.RDFAuthZ
import org.specs._
import dispatch._
import java.io._

import grizzled.file.GrizzledFile._

import org.w3.readwriteweb.utiltest._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import unfiltered.filter.Planify
import unfiltered.netty.{ReceivedMessage, ServerErrorResponse, cycle}
import unfiltered.spec.netty.Started

trait JettyResourceManaged extends Specification with unfiltered.spec.jetty.Served {
  
  def resourceManager: ResourceManager

  val rww = new ReadWriteWeb[HttpServletRequest,HttpServletResponse] {
     val rm = resourceManager
     def manif = manifest[HttpServletRequest]
   }

  def setup = { _.filter(Planify(rww.intent)) }
 
}

/**
 * Netty Resource managed.
 **/
trait ResourceManaged extends Specification with unfiltered.spec.netty.Served {
  import org.jboss.netty.handler.codec.http._

  def resourceManager: ResourceManager

  val rww = new cycle.Plan  with cycle.ThreadPool with ServerErrorResponse with ReadWriteWeb[ReceivedMessage,HttpResponse] {
    val rm = resourceManager
    def manif = manifest[ReceivedMessage]
  }

  def setup = { _.plan(rww) }

}



trait FilesystemBased extends ResourceManaged {

  lazy val mode: RWWMode = ResourcesDontExistByDefault

  lazy val lang = RDFXML

  lazy val baseURL = "/wiki"

  lazy val root = new File(new File(System.getProperty("java.io.tmpdir")), "readwriteweb")

  lazy val resourceManager = new Filesystem(root, baseURL, lang)(mode)

  doBeforeSpec {
    if (root.exists) root.deleteRecursively()
    root.mkdir()
  }

}

trait SomeRDF extends SomeURI {
  
  val rdfxml =
"""
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"> 
  <foaf:Person rdf:about="#JL" xmlns:foaf="http://xmlns.com/foaf/0.1/">
    <foaf:name>Joe Lambda</foaf:name>
    <foaf:homepage rdf:resource="/2007/wiki/people/JoeLambda" />
  </foaf:Person>
</rdf:RDF>
"""
      
  val turtle =
"""
"""
    
  val referenceModel = modelFromString(rdfxml, uriBase, RDFXML).toOption.get

}

trait SomeURI extends FilesystemBased {
  
  val emptyModel = com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel()

  val peopleDirPath = "wiki/people/"

  lazy val dirUri = host / peopleDirPath
  
  lazy val uri = host / "wiki/people/JoeLambda"
  
  lazy val uriBase = baseURI(uri)
  
  lazy val directory = new File(root, "people")
  
  lazy val resourceOnDisk = new File(root, "people/JoeLambda")
  
}

trait SomePeopleDirectory extends SomeRDF with SomeURI {
  
  doBeforeSpec {
    val httpCode = Http(dirUri.put(RDFXML, rdfxml) get_statusCode)
    httpCode must_== 201
  }
  
}

trait SomeDataInStore extends SomePeopleDirectory {
  
  doBeforeSpec {
    val httpCode = Http(uri.put(RDFXML, rdfxml) get_statusCode)
    httpCode must_== 201
  }
  
}
