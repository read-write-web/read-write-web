package org.w3.readwriteweb

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

import dispatch._

object GetStrictModeSpec extends FilesystemBased with SomeRDF with SomeURI {
  
  "a GET on a URL that does not exist" should {
    "return a 404" in {
      val httpCode:Int = Http.when( _ => true)(uri get_statusCode)
      httpCode must_== 404
    }
  }

}

object GetWikiModeSpec extends FilesystemBased with SomeRDF with SomeURI {
  
  override lazy val mode = AllResourcesAlreadyExist
  
  "a GET on a URL that does not exist" should {
    "return a 200 and an empty model" in {
      val (statusCode, model) = Http(uri >+ {
        req => (req.get_statusCode,
                req as_model(uriBase))
      } )
      statusCode must_== 200
      model must beIsomorphicWith (emptyModel)
    }
  }
  
}

object ContentNegociationSpec extends SomeDataInStore {

  "a GET on Joe's URI" should {
    "deliver TURTLE and RDF/XML graphs that are isomorphic to each other" in {
      val rdfxml = Http(uri as_model(uriBase))
      val turtle = Http(uri <:< Map("Accept" -> "text/turtle") as_model(uriBase, lang="TURTLE"))
      rdfxml must beIsomorphicWith(turtle)
    }
  }
  
}
