package org.w3.readwriteweb

import unfiltered.request._

object RequestLang {
  
  def apply(req: HttpRequest[_]): Option[Lang] =
    Lang(RequestContentType(req))

  def unapply(req: HttpRequest[_]): Option[Lang] =
    apply(req)

  def unapply(ct: String): Option[Lang] =
    Lang(ct)
    
}
