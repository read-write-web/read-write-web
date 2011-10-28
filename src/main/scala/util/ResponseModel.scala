package org.w3.readwriteweb

import java.io._
import java.net.URL

import com.hp.hpl.jena.rdf.model._
import unfiltered.response._

object ResponseModel {
  def apply(model: Model, base: URL, lang: Lang): ResponseStreamer =
    new ResponseStreamer {
      def stream(os: OutputStream): Unit =
        model.getWriter(lang.jenaLang).write(model, os, base.toString)
    }
}