
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
      case "jpeg" => ImageRepr(JPEG)
      case "png" => ImageRepr(PNG)
      case "gif" => ImageRepr(GIF)
      case "/" => DirectoryRepr
      case _ => UnknownRepr
    }
  }
  

  val htmlContentTypes = Set("text/html", "application/xhtml+xml")
  val imgageTypes = Set("image/jpeg","image/png","image/gif")
  
  def acceptsHTML(ct: Iterable[String]) =
    ! (htmlContentTypes & ct.toSet).isEmpty

  def acceptsPicture(ct: Iterable[String]) =
    ! (imgageTypes & ct.toSet).isEmpty
  
  def fromAcceptedContentTypes(ct: Iterable[String]): Representation = {
    Lang(ct) map RDFRepr.apply getOrElse {
      Image(ct).map(ImageRepr.apply(_)).getOrElse {
      if (acceptsHTML(ct))
        HTMLRepr
      else
        UnknownRepr
      }
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
case class ImageRepr(mime: Image) extends Representation
case object HTMLRepr extends Representation
case object DirectoryRepr extends Representation
case object UnknownRepr extends Representation
