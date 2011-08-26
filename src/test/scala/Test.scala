package org.w3.readwriteweb

import org.specs._
import java.net.URL
import unfiltered.response._
import unfiltered.request._
import dispatch._
import java.io.File

object ReadWriteWebSpec extends Specification with unfiltered.spec.jetty.Served {

  def post(req:Request, body:String) = (req <<< body).copy(method="POST")

  def setup = { _.filter(new ReadWriteWeb(new File("src/main/resources")).read) }

  val timBL = host / "/People/Berners-Lee/card#i"
    
  "GET on TimBL's FOAF profile" should {
    "return something" in {
      val body:String = Http(timBL as_str)
      body must not be empty
    }
  }

  val update = host / "People/Berners-Lee/card#i"
    
  val sparqlAdd =
"""
PREFIX dc: <http://purl.org/dc/elements/1.1/>
INSERT DATA
{ 
  <http://example/book1> dc:title "A new book" ;
                         dc:creator "A.N.Other" .
}
"""

  "SPARQL UPDATE on TimBL's FOAF profile" should {
    "return something new" in {
      val body:String = Http(post(timBL, sparqlAdd) as_str)
      // println(body)
      body must be matching ".*A new book.*"
//      println("here")
    }
  }
  
}
