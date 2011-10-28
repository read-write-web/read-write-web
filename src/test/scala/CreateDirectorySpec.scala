package org.w3.readwriteweb

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

import dispatch._

object CreateDirSpec extends FilesystemBased with SomeRDF with SomeURI {

  "PUTing an RDF document on /people/" should {
    "return a 201" in {
      val httpCode = Http(dirUri.put(RDFXML, rdfxml) get_statusCode)
      httpCode must_== 201
    }
    "create a directory on disk" in {
      directory must be directory
    }
  }

}
