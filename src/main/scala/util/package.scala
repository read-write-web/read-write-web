package org.w3.readwriteweb

import java.io._
import com.hp.hpl.jena.rdf.model._
import scalaz._
import Scalaz._
import java.net.URL

package object util {
  
  def modelFromInputStream(
      is: InputStream,
      base: URL,
      lang: Lang): Validation[Throwable, Model] =
    try {
      val m = ModelFactory.createDefaultModel()
      m.getReader(lang.jenaLang).read(m, is, base.toString)
      m.success
    } catch {
      case t => t.fail
    }
  
  def modelFromString(
      s: String,
      base: URL,
      lang: Lang): Validation[Throwable, Model] =
    try {
      val reader = new StringReader(s)
      val m = ModelFactory.createDefaultModel()
      m.getReader(lang.jenaLang).read(m, reader, base.toString)
      m.success
    } catch {
      case t => t.fail
    }

  implicit def wrapValidation[E, S](v: Validation[E,S]): ValidationW[E, S] =
    new ValidationW[E, S] { val validation = v }
  
  implicit def unwrap[E, F <: E, S <: E](v: Validation[F,S]): E = v.fold(e => e, s => s)
  
  // I wonder if this is already defined somewhere...
  def trySome[T](body: => T): Option[T] =
    try {
      val res = body;
      if (res == null) None else Option(res)
    } catch {
      case _ => None
    }
  
}
