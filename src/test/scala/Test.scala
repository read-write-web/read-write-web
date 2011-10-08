package org.w3.readwriteweb

import org.specs._
import java.net.URL
import unfiltered.response._
import unfiltered.request._
import dispatch._
import java.io._

import com.codecommit.antixml._
import grizzled.file.GrizzledFile._

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

object ReadWriteWebSpec extends Specification with unfiltered.spec.jetty.Served {

  val base = new File(new File(System.getProperty("java.io.tmpdir")), "readwriteweb")
  val joe = host / "2007/wiki/people/JoeLambda"
  val joeBaseURI = baseURI(joe)
  val joeOnDisk = new File(base, "people/JoeLambda")
  
  //base.deleteRecursively()

  doBeforeSpec {
    if (base.exists)
      base.deleteRecursively()
    base.mkdir()
  }
  
  doAfterSpec {
//    if (joeOnDisk.exists) joeOnDisk.delete()
  }
  
  val filesystem = new Filesystem(base, "/2007/wiki", "N3")(ResourcesDontExistByDefault)

  def setup = { _.filter(new ReadWriteWeb(filesystem).read) }
    
  val joeRDF =
"""
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"> 
  <foaf:Person rdf:about="#JL" xmlns:foaf="http://xmlns.com/foaf/0.1/">
    <foaf:name>Joe Lambda</foaf:name>
    <foaf:homepage rdf:resource="/2007/wiki/people/JoeLambda" />
  </foaf:Person>
</rdf:RDF>
"""
  
  val initialModel = modelFromString(joeRDF, joeBaseURI).toOption.get

  "a GET on a URL that does not exist" should {
    "return a 404" in {
      val httpCode:Int = Http.when( _ => true)(joe get_statusCode)
      httpCode must_== 404
    }
  }
  
  "PUTing an RDF document on Joe's URI (which does not exist yet)" should {
    "return a 201" in {
      val httpCode:Int = Http(joe.put(joeRDF) get_statusCode)
      httpCode must_== 201
    }
    "create a document on disk" in {
      joeOnDisk must exist
    }
  }
  
  "Joe's URI" should {
    "now exist and be isomorphic with the original document" in {
      val (statusCode, via, model) = Http(joe >++ { req => (req.get_statusCode,
                                                            req.get_header("MS-Author-Via"),
                                                            req as_model(joeBaseURI))
                                                  } )
      statusCode must_== 200
      via must_== "SPARQL"
      model must beIsomorphicWith (initialModel)
    }
  }
  
  val insertQuery =
"""
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
INSERT DATA { </2007/wiki/people/JoeLambda#JL> foaf:openid </2007/wiki/people/JoeLambda> }
"""
  
  "POSTing an INSERT query on Joe's URI (which does not exist yet)" should {
    "succeed" in {
      val httpCode:Int = Http(joe.post(insertQuery) get_statusCode)
      httpCode must_== 200
    }
    "produce a graph with one more triple than the original one" in {
      val model = Http(joe as_model(joeBaseURI))
      model.size must_== (initialModel.size + 1)
    }
  }

  "a GET on Joe's URI" should {
    "deliver TURTLE and RDF/XML graphs that are isomorphic to each other" in {
      val rdfxml = Http(joe as_model(joeBaseURI))
      val turtle = Http(joe <:< Map("Accept" -> "text/turtle") as_model(joeBaseURI, lang="TURTLE"))
      rdfxml must beIsomorphicWith(turtle)
    }
  }
  
  val diffRDF =
"""
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"> 
  <foaf:Person rdf:about="#JL" xmlns:foaf="http://xmlns.com/foaf/0.1/">
    <foaf:img rdf:resource="/2007/wiki/people/JoeLambda/images/me.jpg" />
  </foaf:Person>
</rdf:RDF>
"""

  val finalRDF =
"""
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"> 
  <foaf:Person rdf:about="#JL" xmlns:foaf="http://xmlns.com/foaf/0.1/">
    <foaf:name>Joe Lambda</foaf:name>
    <foaf:homepage rdf:resource="/2007/wiki/people/JoeLambda" />
    <foaf:openid rdf:resource="/2007/wiki/people/JoeLambda" />
    <foaf:img rdf:resource="/2007/wiki/people/JoeLambda/images/me.jpg" />
  </foaf:Person>
</rdf:RDF>
"""  
    
  val expectedFinalModel = modelFromString(finalRDF, joeBaseURI).toOption.get

  "POSTing an RDF document to Joe's URI" should {
    "succeed" in {
      val httpCode:Int = Http(joe.post(diffRDF) get_statusCode)
      httpCode must_== 200
    }
    "append the diff graph to the initial graph" in {
      val model = Http(joe as_model(joeBaseURI))
      model must beIsomorphicWith (expectedFinalModel)
    }
  }

  "POSTing an RDF document to a resource that does not exist" should {
    val doesnotexist = host / "2007/wiki/doesnotexist"
    "return a 404" in {
      val httpCode:Int = Http.when( _ => true)(doesnotexist get_statusCode)
      httpCode must_== 404
    }
  }
  
  val selectFoafName =
"""
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name WHERE { [] foaf:name ?name }
"""
  
  """POSTing "SELECT ?name WHERE { [] foaf:name ?name }" to Joe's URI""" should {
    "return Joe's name" in {
      val resultSet = Http(joe.post(selectFoafName) >- { body => ResultSetFactory.fromXML(body) } )
      resultSet.next().getLiteral("name").getString must_== "Joe Lambda"
    }
  }
  
  val askFoafName =
"""
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
ASK { [] foaf:name ?name }
"""
  
  """POSTing "ASK ?name WHERE { [] foaf:name ?name }" to Joe's URI""" should {
    "return true" in {
      val result:Boolean =
        Http(joe.post(askFoafName) >~ { s => 
          (XML.fromSource(s) \ "boolean" \ text).head.toBoolean
          } )
      result must_== true
    }
  }
  
  val constructIdentity =
"""
CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }
"""
  
  """POSTing "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }" to Joe's URI""" should {
    "return an isomorphic RDF graph" in {
      val model = Http(joe as_model(joeBaseURI))
      model must beIsomorphicWith (expectedFinalModel)
    }
  }
  
  """POSTing something that does not make sense to Joe's URI""" should {
    "return a 400 Bad Request" in {
      val statusCode = Http.when(_ == 400)(joe.post("that's bouleshit") get_statusCode)
      statusCode must_== 400
    }
  }
  
  """PUTting not-valid RDF to Joe's URI""" should {
    "return a 400 Bad Request" in {
      val statusCode = Http.when(_ == 400)(joe.put("that's bouleshit") get_statusCode)
      statusCode must_== 400
    }
  }

  """a DELETE request""" should {
    "not be supported yet" in {
      val statusCode = Http.when(_ == 405)(joe.copy(method="DELETE") get_statusCode)
      statusCode must_== 405
    }
  }
}
