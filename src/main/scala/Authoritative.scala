package org.w3.readwriteweb

import unfiltered.request._
import java.net.URL

sealed trait Representation

object Representation {
  
  def fromSuffix(suffix: String): Representation = {
    suffix match {
      case "n3" => RDFRepr(N3)
      case "turtle" | "ttl" => RDFRepr(TURTLE)
      case "rdf" => RDFRepr(RDFXML)
      case "htm" | "html" | "xhtml" => HTMLRepr
      case _ => UnknownRepr
    }
  }
  
  val htmlCharsets = Set("text/html", "application/xhtml+xml")
  
  def acceptsHTML(ct: Iterable[String]) =
    ! (htmlCharsets & ct.toSet).isEmpty
  
  def fromAcceptedContentTypes(ct: Iterable[String]): Representation = {
    Lang(ct) map RDFRepr.apply getOrElse {
      if (acceptsHTML(ct))
        HTMLRepr
      else
        UnknownRepr
    }
  }
  
  /** implements http://www.w3.org/2001/tag/doc/metaDataInURI-31 and http://www.w3.org/2001/tag/doc/mime-respect
    * 
    * if there is no known suffix (eg. the URI was already the authoritative one),
    * inspects the given accepted content types
    * 
    * This knows only about the RDF and HTML charsets
    */
  def apply(
      suffixOpt: Option[String],
      ct: Iterable[String]): Representation = {
    suffixOpt map fromSuffix match {
      case None | Some(UnknownRepr) => fromAcceptedContentTypes(ct)
      case Some(repr) => repr
    }
  }
}

case class RDFRepr(lang: Lang) extends Representation
case object HTMLRepr extends Representation
case object UnknownRepr extends Representation
case object NoRepr extends Representation

object Authoritative {
  
  val r = """^(.*)\.(\w{0,4})$""".r
  
  def unapply(req: HttpRequest[javax.servlet.http.HttpServletRequest]): Option[(URL, Representation)] = {
    val uri = req.underlying.getRequestURL.toString
    val suffixOpt = uri match {
      case r(_, suffix) => Some(suffix)
      case _ => None
    }
    Some((new URL(uri), Representation(suffixOpt, Accept(req))))
  }
}
