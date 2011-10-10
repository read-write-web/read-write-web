package org.w3.readwriteweb

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

import dispatch._

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._

import com.codecommit.antixml._

object PostSelectSpec extends SomeDataInStore {

  val selectFoafName =
"""
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name WHERE { [] foaf:name ?name }
"""
  
  """POSTing "SELECT ?name WHERE { [] foaf:name ?name }" to Joe's URI""" should {
    "return Joe's name" in {
      val resultSet = Http(uri.post(selectFoafName) >- { body => ResultSetFactory.fromXML(body) } )
      resultSet.next().getLiteral("name").getString must_== "Joe Lambda"
    }
  }
  
}


object PostAskSpec extends SomeDataInStore {

  val askFoafName =
"""
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
ASK { [] foaf:name ?name }
"""
  
  """POSTing "ASK ?name WHERE { [] foaf:name ?name }" to Joe's URI""" should {
    "return true" in {
      val result: Boolean =
        Http(uri.post(askFoafName) >~ { s => 
          (XML.fromSource(s) \ "boolean" \ text).head.toBoolean
          } )
      result must_== true
    }
  }
  
}

object PostConstructSpec extends SomeDataInStore {

  val constructIdentity =
"""
CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }
"""
  
  """POSTing "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }" to Joe's URI""" should {
    "return an isomorphic RDF graph" in {
      val model = Http(uri as_model(uriBase))
      model must beIsomorphicWith (referenceModel)
    }
  }

}
