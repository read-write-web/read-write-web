package org.w3.readwriteweb

import javax.servlet._
import javax.servlet.http._
import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

import java.io._
import scala.io.Source

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._
import com.hp.hpl.jena.shared.JenaException

import org.w3.readwriteweb.util._

// holds some Unfiltered plans
class ReadWriteWeb(base:File) {
  
  val logger:Logger = LoggerFactory.getLogger(this.getClass)

  val echo = unfiltered.filter.Planify {
    case Path(Seg("echo" :: p :: Nil)) => ResponseString(p)
  }
  
  val read = unfiltered.filter.Planify {
    case req @ Path(path) => {
      val baseURI = req.underlying.getRequestURL.toString  
      val fileOnDisk = new File(base, path)
       
      def foo():(OutputStream, Model) = {
        // get empty model if file not on disk
        val model = ModelFactory.createDefaultModel()
        if (fileOnDisk exists) {
          val fis = new FileInputStream(fileOnDisk)
          model.read(fis, baseURI)
          fis.close()
        }
        // if file does not exist, create it
        if (! fileOnDisk.exists) {
          // create parent directory if needed
          val parent = fileOnDisk.getParentFile
          if (! parent.exists) println(parent.mkdirs)
          val r = fileOnDisk.createNewFile()
          logger.debug("Create file %s with success: %s" format
              (fileOnDisk.getAbsolutePath, r.toString))
        }
        val fos = new FileOutputStream(fileOnDisk)
        (fos, model)
      }
      def loadModel(file:File):Model = {
        val fis = new FileInputStream(fileOnDisk)
        val m = ModelFactory.createDefaultModel()
        try {
          m.read(fis, baseURI)
        } catch {
          case je:JenaException => logger.error("File %s was either empty or corrupted: considered as empty graph" format fileOnDisk.getAbsolutePath)
        }
        fis.close()
        m
      }
      req match {
        case GET(_) => {
          val model:Model = loadModel(fileOnDisk)
          Ok ~> ViaSPARQL ~> ResponseModel(model, baseURI)
        }
        case PUT(_) => {
          val bodyModel = modelFromInputStream(Body.stream(req), baseURI)
          val (fos, _) = foo()
          bodyModel.write(fos, "RDF/XML-ABBREV", baseURI)
          fos.close()
          Ok ~> ResponseString("")
        }
        case POST(_) => {
          /* http://openjena.org/ARQ/javadoc/com/hp/hpl/jena/update/UpdateFactory.html */
          val update:UpdateRequest = UpdateFactory.read(Body.stream(req))
          val (fos, model) = foo()
          UpdateAction.execute(update, model)
          model.write(fos)
          fos.close()
          // give back the modified model
          Ok ~> new ResponseStreamer {
            def stream(os:OutputStream):Unit = {
              val lang = "RDF/XML-ABBREV"
              model.write(os, lang)
            }
          }
        }
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