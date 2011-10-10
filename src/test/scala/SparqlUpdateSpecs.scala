package org.w3.readwriteweb

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

import dispatch._

object PostInsertSpec extends SomeDataInStore {

  val insertQuery =
"""
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
INSERT DATA { </2007/wiki/people/JoeLambda#JL> foaf:openid </2007/wiki/people/JoeLambda> }
"""
  
  "POSTing an INSERT query on Joe's URI (which does not exist yet)" should {
    "succeed" in {
      val httpCode = Http(uri.post(insertQuery) get_statusCode)
      httpCode must_== 200
    }
    "produce a graph with one more triple than the original one" in {
      val model = Http(uri as_model(uriBase))
      model.size must_== (referenceModel.size + 1)
    }
  }
  
}
