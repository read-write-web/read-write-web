package org.w3.readwriteweb

import javax.servlet._
import javax.servlet.http._
import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

import java.io._
import scala.io.Source

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._

import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

import dispatch._

import org.specs.matcher.Matcher

import org.w3.readwriteweb.util._

package object utiltest {
  
  def beIsomorphicWith(that:Model):Matcher[Model] =
    new Matcher[Model] {
      def apply(otherModel: => Model) =
        (that isIsomorphicWith otherModel,
         "Model A is isomorphic to model B",
         "%s not isomorphic with %s" format (otherModel.toString, that.toString))
  }
  
  class RequestW(req:Request) {

    def as_model(base:String, lang:String = "RDF/XML-ABBREV"):Handler[Model] =
      req >> { is => modelFromInputStream(is, base) }

    def post(body:String):Request =
      (req <<< body).copy(method="POST")
      
    def put(body:String):Request = req <<< body
      
    def get_statusCode:Handler[Int] = new Handler(req, (c, r, e) => c, null)
    
    def get:Request = req.copy(method="GET")
    
  }
  
  implicit def wrapRequest(req:Request):RequestW = new RequestW(req)
  




}