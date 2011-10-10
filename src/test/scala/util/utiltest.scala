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
  
  def baseURI(req:Request):String = "%s%s" format (req.host, req.path)
  
  def beIsomorphicWith(that:Model):Matcher[Model] =
    new Matcher[Model] {
      def apply(otherModel: => Model) =
        (that isIsomorphicWith otherModel,
         "Model A is isomorphic to model B",
         "%s not isomorphic with %s" format (otherModel.toString, that.toString))
  }
  
  class RequestW(req:Request) {

    def as_model(base:String, lang:String = "RDF/XML-ABBREV"):Handler[Model] =
      req >> { is => modelFromInputStream(is, base, lang).toOption.get }

    def post(body:String):Request =
      (req <<< body).copy(method="POST")
      
    def put(body:String):Request = req <<< body
      
    def get_statusCode:Handler[Int] = new Handler(req, (c, r, e) => c, { case t => () })
    
    def get_header(header:String):Handler[String] = req >:> { _(header).head }
    
    def get:Request = req.copy(method="GET")
    
    def >++ [A, B, C] (block: Request => (Handler[A], Handler[B], Handler[C])) = {
      Handler(req, { (code, res, opt_ent) =>
        val (a, b, c) = block( /\ )
          (a.block(code, res, opt_ent), b.block(code,res,opt_ent), c.block(code,res,opt_ent))
      } )
    }
    
    def >+ [A, B] (block: Request => (Handler[A], Handler[B])) = {
      Handler(req, { (code, res, opt_ent) =>
        val (a, b) = block( /\ )
        (a.block(code, res, opt_ent), b.block(code,res,opt_ent))
      } )
    }
    
  }
  
  implicit def wrapRequest(req:Request):RequestW = new RequestW(req)
  




}