package org.w3.readwriteweb

import org.w3.readwriteweb.util._
import org.w3.readwriteweb.utiltest._

import dispatch._

object PostBouleshitSpec extends SomeDataInStore {

  """POSTing something that does not make sense to Joe's URI""" should {
    "return a 400 Bad Request" in {
      val statusCode = Http.when(_ == 400)(uri.post("that's bouleshit") get_statusCode)
      statusCode must_== 400
    }
  }
  
}


object DeleteResourceSpec extends SomeDataInStore {

  """a DELETE request""" should {
    "not be supported yet" in {
      val statusCode = Http.when(_ == 405)(uri.copy(method="DELETE") get_statusCode)
      statusCode must_== 405
    }
  }

}
