package org.w3.readwriteweb.util

import unfiltered.request._

// TODO pull request to the unfiltered project!
object HttpMethod {
  def unapply(req: HttpRequest[_]): Option[Method] =
    Some(
      req.method match {
        case "GET" => GET
        case "PUT" => PUT
        case "HEAD" => HEAD
        case "POST" => POST
        case "CONNECT" => CONNECT
        case "OPTIONS" => OPTIONS
        case "TRACE" => TRACE
        case m => new Method(m)
      })

}