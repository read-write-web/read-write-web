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

import org.w3.readwriteweb.utiltest._

object ReadWriteWebSpec extends Specification with unfiltered.spec.jetty.Served {

  val base = new File("src/main/resources")
  val joe = host / "2007/wiki/people/JoeLambda"
  val joeOnDisk = new File(base, "2007/wiki/people/JoeLambda")
  
  doBeforeSpec {
    if (joeOnDisk.exists) joeOnDisk.delete()    
  }
  
  doAfterSpec {
//    if (joeOnDisk.exists) joeOnDisk.delete()
  }
  
  def setup = { _.filter(new ReadWriteWeb(base).read) }

  val timBL = host / "People/Berners-Lee/card#i"
  
  "a GET on TimBL's FOAF profile" should {
    val (via, body) = Http(timBL >+ { req =>
      (req >:> { _("MS-Author-Via").head }, req as_str)
    } )
    "return an non empty document" in {
      body must not be empty
    }
    """have the header "MS-Author-Via" set to SPARQL""" in {
      via must_== "SPARQL"
    }
  }

    
  val insertQuery =
"""
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
INSERT DATA { <http://dig.csail.xvm.mit.edu/2007/wiki/people/JoeLambda#JL> foaf:name "Joe Lambda" }
"""
        
  "POSTing an INSERT query on Joe's URI (which does not exist yet)" should {
    "return a 200" in {
      val httpCode:Int = Http(joe.post(insertQuery) get_statusCode)
      httpCode must_== 200
    }
    "create the corresponding file on disk" in {
      joeOnDisk must exist
    }
    "create a valid rdf document with exactly one triple" in {
      val model = Http(joe as_model(joe.path) )
      model.size must_== 1
    }
  }
  
  val joeRDF =
"""
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"> 
  <foaf:Person rdf:about="#JL" xmlns:foaf="http://xmlns.com/foaf/0.1/">
    <foaf:name>Joe Lambda</foaf:name>
    <foaf:homepage rdf:resource="/2007/wiki/people/JoeLambda" />
  </foaf:Person>
</rdf:RDF>
"""

//        <foaf:openid rdf:resource="/2007/wiki/people/JoeLambda" />
//    <foaf:img rdf:resource="/2007/wiki/people/JoeLambda/images/me.jpg" />

//  "PUTing an RDF document on Joe's URI (which now does exist)" should {
//    "return a 200" in {
//      val httpCode:Int = Http(joe.post(joeRDF) get_statusCode)
//      httpCode must_== 200
//    }
//    "create a valid rdf document with exactly XXX triple" in {
//      val model = Http(joe as_model(joe.path))
//      model.size must_== 1
//    }
//  }
    
    
    
}
