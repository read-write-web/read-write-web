package org.w3.readwriteweb

import org.specs._
import java.net.URL
import unfiltered.response._
import unfiltered.request._
import dispatch._
import java.io.File

object ReadWriteWebSpec extends Specification with unfiltered.spec.jetty.Served {

  def setup = { _.filter(new ReadWriteWeb(new File("src/main/resources")).read) }

  val get:Request = host / "/People/Berners-Lee/card#i"
    
  "GET on TimBL's FOAF profile" should {
    "return something" in {
      val body:String = Http(get as_str)
      body must not be empty
    }
  }
  
}
