package org.w3.readwriteweb

import org.w3.yourapp.util.ResourceManager.fromClasspath

import javax.servlet._
import javax.servlet.http._
import unfiltered.request._
import unfiltered.response._
import unfiltered.scalate._
import unfiltered.jetty._

import java.io._
import scala.io.Source

import org.slf4j.{Logger, LoggerFactory}
import com.hp.hpl.jena.rdf.model._

// holds some Unfiltered plans
class ReadWriteWeb(base:File) {
  
  val logger:Logger = LoggerFactory.getLogger(this.getClass)

  val echo = unfiltered.filter.Planify {
    case Path(Seg("echo" :: p :: Nil)) => ResponseString(p)
  }

  val read = unfiltered.filter.Planify {
    case req @ Path(path) => {
      val fis = new FileInputStream(new File(base, path))
      /* http://jena.sourceforge.net/tutorial/RDF_API/index.html#ch-Reading%20RDF */
      val model:Model = ModelFactory.createDefaultModel()
      model.read(fis, null)
      req match {
        case GET(_) => {
          Ok ~> new ResponseStreamer {
            def stream(os:OutputStream):Unit = {
              /* http://jena.sourceforge.net/tutorial/RDF_API/index.html#ch-Writing%20RDF */
              // val lang = "RDF/XML-ABBREV"
              val lang = "TURTLE"
              model.write(os, lang)
            }
          }
        }
        // case POST(_) => {
        //   val query = 
        // }
      }
    }
  }

}

object ReadWriteWebMain {

  val logger:Logger = LoggerFactory.getLogger(this.getClass)

  // regular Java main
  def main(args: Array[String]) {
    
    // can provide the port as first argument
    // default to 2719
    val (port, directory) = args.toList match {
      case port :: directory :: Nil => (port.toInt, directory)
      case _ => sys.error("wrong arguments")
    }

    val app = new ReadWriteWeb(new File(directory))

    // configures and launches a Jetty server
    unfiltered.jetty.Http(port).filter {
      // a jee Servlet filter that logs request
      new Filter {
        def destroy():Unit = ()
        def doFilter(request:ServletRequest, response:ServletResponse, chain:FilterChain):Unit = {
          val r:HttpServletRequest = request.asInstanceOf[HttpServletRequest]
          val method = r.getMethod
          val uri = r.getRequestURI 
          logger.info("%s %s" format (method, uri))
          chain.doFilter(request, response)
        }
        def init(filterConfig:FilterConfig):Unit = ()
      }
    // Unfiltered filters
    }.filter(app.read).run()
    
  }

}
