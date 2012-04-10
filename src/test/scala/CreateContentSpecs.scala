package org.w3.readwriteweb

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

import dispatch._
import java.net.URL
import java.io.File

object PutRDFXMLSpec extends SomePeopleDirectory {

  "PUTing an RDF document on Joe's URI (which does not exist yet)" should {
    "return a 201" in {
      val httpCode = Http(uri.put(RDFXML, rdfxml) get_statusCode)
      httpCode must_== 201
    }
    "create a document on disk" in {
      resourceOnDisk must exist
    }
  }
  
  "Joe's URI" should {
    "now exist and be isomorphic with the original document" in {
      val (statusCode, via, model) = Http(uri >++ { req => (req.get_statusCode,
                                                            req.get_header("MS-Author-Via"),
                                                            req as_model(uriBase, RDFXML))
                                                  } )
      statusCode must_== 200
      via must_== "SPARQL"
      model must beIsomorphicWith (referenceModel)
    }
  }
  
}


object PostRDFSpec extends SomeDataInStore {
  
    val diffRDF =
"""
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"> 
  <foaf:Person rdf:about="#JL" xmlns:foaf="http://xmlns.com/foaf/0.1/">
    <foaf:openid rdf:resource="/2007/wiki/people/JoeLambda" />
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
    
  val expectedFinalModel = modelFromString(finalRDF, uriBase, RDFXML).toOption.get

  "POSTing an RDF document to Joe's URI" should {
    "succeed" in {
      val httpCode:Int = Http(uri.post(diffRDF, RDFXML) get_statusCode)
      httpCode must_== 200
    }
    "append the diff graph to the initial graph" in {
      val model = Http(uri as_model(uriBase, RDFXML))
      model must beIsomorphicWith (expectedFinalModel)
    }
  }

  "POSTing an RDF document to a Joe's directory/collection" should {
    "succeed and create a resource on disk" in {
      val handler = dirUri.post(diffRDF, RDFXML) >+ { req =>
          val loc: Handler[String] = req.get_header("Location")
          val status_code: Handler[Int] = req.get_statusCode
          (status_code,loc)
      }
      val (code, head) = Http(handler)
      System.out.println("code="+code)
      System.out.println("head="+head)
      code must_== 201
      val headURI = new URL(head.trim)
      System.out.println("root="+root)
      val file = new File(root, headURI.getPath.substring(baseURL.size))
      file must exist
    }

    "create a resource on disk" in {
//      joeProfileOnDisk must be file
    }
  }
  
}


object PutInvalidRDFXMLSpec extends SomeDataInStore {

  """PUTting not-valid RDF to Joe's URI""" should {
    "return a 400 Bad Request" in {
      val statusCode = Http.when(_ == 400)(uri.put(RDFXML, "that's bouleshit") get_statusCode)
      statusCode must_== 400
    }
  }
  
}

object PostOnNonExistingResourceSpec extends FilesystemBased {

  "POSTing an RDF document to a resource that does not exist" should {
    val doesnotexist = host / "2007/wiki/doesnotexist"
    "return a 404" in {
      val httpCode:Int = Http.when( _ => true)(doesnotexist get_statusCode)
      httpCode must_== 404
    }
  }

}
