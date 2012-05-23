package org.w3.readwriteweb

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

import dispatch._
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.vocabulary.RDF

object CreateDirSpec extends FilesystemBased with SomeRDF with SomeURI {

  "PUTing an RDF document on /people/" should {
    "return a 201" in {
      val httpCode = Http(dirUri.put(RDFXML, rdfxml) get_statusCode)
      httpCode must_== 201
    }
    "create a directory on disk" in {
      directory must be directory
    }
    "return a directory listing" in {
      val (statusCode, model): Pair[Int,Model] = Http(dirUri >+ {
        req => (req.get_statusCode,
          req as_model(uriBase, RDFXML))
      } )
      val sioc = "http://rdfs.org/sioc/ns#"
      statusCode must_== 200
      model.contains(model.createResource(uri.to_uri.toString),RDF.`type`,model.createResource(sioc+"Container")) mustBe true
      model.listStatements(null,RDF.`type`,model.createResource(sioc+"Item")).hasNext mustBe false
    }
  }

}
