package org.w3.readwriteweb

import javax.servlet._
import javax.servlet.http._
import unfiltered.jetty._
import java.io.File
import org.slf4j.{Logger, LoggerFactory}

/** a simple JEE Servlet filter that logs HTTP requests
 */
class FilterLogger(logger: Logger) extends Filter {

  def destroy(): Unit = ()

  def doFilter(
    request: ServletRequest,
    response: ServletResponse,
    chain: FilterChain): Unit = {
    val r: HttpServletRequest = request.asInstanceOf[HttpServletRequest]
    val method = r.getMethod
    val uri = r.getRequestURI 
    logger.info("%s %s" format (method, uri))
    chain.doFilter(request, response)
  }

  def init(filterConfig: FilterConfig): Unit = ()

}
