package org.w3.readwriteweb

import unfiltered.request._

object AcceptLang {
  
  def unapply(req: HttpRequest[_]): Option[Lang] =
    Accept(req) map Lang.apply collectFirst { case Some(lang) => lang }
  
  def apply(req: HttpRequest[_]): Option[Lang] =
    unapply(req)

}
