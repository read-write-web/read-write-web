package org.w3.readwriteweb

import javax.servlet._
import javax.servlet.http._
import unfiltered.request._
import unfiltered.response._
import unfiltered.jetty._

import java.io._
import scala.io.Source
import java.net.URL

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.query._
import com.hp.hpl.jena.update._
import com.hp.hpl.jena.shared.JenaException
import Query.{QueryTypeSelect => SELECT, QueryTypeAsk => ASK,
              QueryTypeConstruct => CONSTRUCT, QueryTypeDescribe => DESCRIBE}

import org.w3.readwriteweb.util._

class ReadWriteWeb(rm:ResourceManager) {
  
  val logger:Logger = LoggerFactory.getLogger(this.getClass)

  val read = unfiltered.filter.Planify {
    case req @ Path(path) => {
      val baseURI = req.underlying.getRequestURL.toString
      val r:Resource = rm.resource(new URL(baseURI))
      req match {
        case GET(_) | HEAD(_) =>
          try {
            val model:Model = r.get()
            val encoding = RDFEncoding(req)
            req match {
              case GET(_) => Ok ~> ViaSPARQL ~> ResponseModel(model, baseURI, encoding)
              case HEAD(_) => Ok ~> ViaSPARQL
            }
          } catch {
            case fnfe:FileNotFoundException => NotFound
            case t:Throwable => {
              req match {
                case GET(_) => InternalServerError ~> ViaSPARQL
                case HEAD(_) => InternalServerError ~> ViaSPARQL ~> ResponseString(t.getStackTraceString)
              }
            }
          }
        case PUT(_) =>
          try {
            val bodyModel = modelFromInputStream(Body.stream(req), baseURI)
            r.save(bodyModel)
            Created
          } catch {
            case t:Throwable => InternalServerError ~> ResponseString(t.getStackTraceString)
          }
        case POST(_) =>
          try {
            Post.parse(Body.stream(req), baseURI) match {
              case PostUnknown =>
                BadRequest ~> ResponseString("You MUST provide valid content for either: SPARQL UPDATE, SPARQL Query, RDF/XML, TURTLE")
              case PostUpdate(update) => {
                val model = r.get()
                UpdateAction.execute(update, model)
                r.save(model)
                Ok
              }
              case PostRDF(diffModel) => {
                val model = r.get()
                model.add(diffModel)
                r.save(model)
                Ok
              }
              case PostQuery(query) => {
                lazy val encoding = RDFEncoding(req)
                val model:Model = r.get()
                val qe:QueryExecution = QueryExecutionFactory.create(query, model)
                query.getQueryType match {
                  case SELECT =>
                    Ok ~> ResponseResultSet(qe.execSelect())
                  case ASK =>
                    Ok ~> ResponseResultSet(qe.execAsk())
                  case CONSTRUCT => {
                    val result:Model = qe.execConstruct()
                    Ok ~> ResponseModel(model, baseURI, encoding)
                  }
                  case DESCRIBE => {
                    val result:Model = qe.execDescribe()
                    Ok ~> ResponseModel(model, baseURI, encoding)
                  }
                }
              }
            }
          } catch {
            case fnfe:FileNotFoundException => NotFound
            case t:Throwable => InternalServerError ~> ResponseString(t.getStackTraceString)
          }
        case _ => MethodNotAllowed ~> Allow("GET", "PUT", "POST")
      }
    }

  }

}


object ReadWriteWebMain {

  val logger:Logger = LoggerFactory.getLogger(this.getClass)

  // regular Java main
  def main(args: Array[String]) {
    
    val (port, baseDirectory, baseURL) = args.toList match {
      case port :: directory :: base :: Nil => (port.toInt, new File(directory), base)
      case _ => {
        println("example usage:\n\tjava -jar read-write-web.jar 8080 ~/WWW/2011/09 /2011/09")
        System.exit(1)
        null
      }
    }

    if (! baseDirectory.exists) {
      println("%s does not exist" format (baseDirectory.getAbsolutePath))
      System.exit(2)
    }

    val filesystem = new Filesystem(baseDirectory, baseURL)(ResourcesDontExistByDefault)
    
    val app = new ReadWriteWeb(filesystem)

    // configures and launches a Jetty server
    unfiltered.jetty.Http(port).filter {
      // a jee Servlet filter that logs HTTP requests
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
