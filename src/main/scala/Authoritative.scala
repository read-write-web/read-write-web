package org.w3.readwriteweb

import unfiltered.request._
import java.net.URL

object Authoritative {
  
  val r = """^(.*)\.(\w{0,4})$""".r
  
  def unapply(req: HttpRequest[javax.servlet.http.HttpServletRequest]): Option[(URL, Representation)] = {
    val uri = req.underlying.getRequestURL.toString
    val suffixOpt = uri match {
      case r(_, suffix) => Some(suffix)
      case _ if uri endsWith "/" => Some("/")
      case _ => None
    }
    Some((new URL(uri), Representation(suffixOpt, Accept(req))))
  }

}
