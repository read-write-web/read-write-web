package org.w3.readwriteweb

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

import dispatch._

object PostBouleshitSpec extends SomeDataInStore {

  """POSTing something that does not make sense to Joe's URI""" should {
    "return a 400 Bad Request" in {
      val statusCode = Http.when(_ == 400)(uri.post("that's bullshit", RDFXML) get_statusCode)  //bulls and bears on the stock exchange
      statusCode must_== 400
    }
  }
  
}


object DeleteResourceSpec extends SomeDataInStore {


  "DELETEing Joe's URI" should {
    "before doing it his resource must" in {
      "be created and return a 201" in {
        val httpCode = Http(uri.put(RDFXML, rdfxml) get_statusCode)
        httpCode must_== 201
      }
      "create a document on disk" in {
        resourceOnDisk must exist
      }
    }

    "succeed" in {
      val httpCode:Int = Http(uri.delete get_statusCode)
      httpCode must_== 204
    }

    "delete the document on disk" in {
      resourceOnDisk mustNot exist
    }
  }


}
