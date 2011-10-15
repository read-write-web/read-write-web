package org.w3.readwriteweb

import java.io._
import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import unfiltered.response._
import scalaz._
import Scalaz._

object ResponseResultSet {

  def apply(rs: ResultSet): ResponseStreamer =
    new ResponseStreamer {
      def stream(os: OutputStream): Unit = ResultSetFormatter.outputAsXML(os, rs) 
    }
  
  def apply(result: Boolean): ResponseStreamer =
    new ResponseStreamer {
      def stream(os: OutputStream): Unit =
        ResultSetFormatter.outputAsXML(os, result) 
    }

}