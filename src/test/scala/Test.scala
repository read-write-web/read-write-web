package org.w3.readwriteweb

import org.specs._
import java.net.URL
import unfiltered.response._
import unfiltered.request._
import dispatch._
import java.io._

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._

// a Handler that returns the http code
object HttpCode {
  def apply(req:Request):Handler[Int] = new Handler(req, (c, r, e) => c, null)
}

object ReadWriteWebSpec extends Specification with unfiltered.spec.jetty.Served {

  val base = new File("src/main/resources")
  
  def post(req:Request, body:String) = (req <<< body).copy(method="POST")

  def setup = { _.filter(new ReadWriteWeb(base).read) }

  val timBL = host / "People/Berners-Lee/card#i"
    
  "GET on TimBL's FOAF profile" should {
    "return something" in {
      val body:String = Http(timBL as_str)
      body must not be empty
    }
  }

  val joe = host / "2007/wiki/people/JoeLambda"
  val joeOnDisk = new File(base, "2007/wiki/people/JoeLambda")
  if (joeOnDisk.exists) joeOnDisk.delete()
    
  val insert =
"""
INSERT DATA { <http://dig.csail.xvm.mit.edu/2007/wiki/people/JoeLambda#JL> <http://xmlns.com/foaf/0.1/age> 66 }
"""
        
  "INSERT query on Joe's URI" should {
    "return a 200" in {
      val httpCode:Int = Http(HttpCode(post(joe, insert)))
      httpCode must_== 200
    }
    "create the corresponding file on disk" in {
      joeOnDisk must exist
    }
    "create a valid rdf document with exactly one triple" in {
      val model = Http(joe >> { is => {
        val m = ModelFactory.createDefaultModel()
        m.read(is, joe.path)
        m
      } } )
      model.size must_== 1
    }
  }

  if (joeOnDisk.exists) joeOnDisk.delete()
  
}
