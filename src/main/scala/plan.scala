package org.w3.readwriteweb

import auth.{AuthZ, NullAuthZ}
import org.w3.readwriteweb.util._

import unfiltered.request._
import unfiltered.response._

import scala.io.Source
import java.net.URL

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.query.{Query, QueryExecution, QueryExecutionFactory}
import com.hp.hpl.jena.update.UpdateAction
import Query.{QueryTypeSelect => SELECT,
              QueryTypeAsk => ASK,
              QueryTypeConstruct => CONSTRUCT,
              QueryTypeDescribe => DESCRIBE}

import scalaz._

class ReadWriteWeb(rm: ResourceManager, implicit val authz: AuthZ = NullAuthZ) {
  
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def isHTML(accepts: List[String]): Boolean = {
    val accept = accepts.headOption
    accept == Some("text/html") || accept == Some("application/xhtml+xml")
  }
  
  /** I believe some documentation is needed here, as many different tricks
   *  are used to make this code easy to read and still type-safe
   *  
   *  Planify.apply takes an Intent, which is defined in Cycle by
   *  type Intent [-A, -B] = PartialFunction[HttpRequest[A], ResponseFunction[B]]
   *  the corresponding syntax is: case ... => ...
   *  
   *  this code makes use of ScalaZ Validation. For example of how to use it, see
   *  http://scalaz.googlecode.com/svn/continuous/latest/browse.sxr/scalaz/example/ExampleValidation.scala.html
   *  
   *  the Resource abstraction returns Validation[Throwable, ?something]
   *  we use the for monadic constructs (although it's *not* a monad).
   *  Everything construct are mapped to Validation[ResponseFunction, ResponseFuntion],
   *  the left value always denoting the failure. Hence, the rest of the for-construct
   *  is not evaluated, but let the reader of the code understand clearly what's happening.
   *  
   *  This mapping is made possible with the failMap method. I couldn't find an equivalent
   *  in the ScalaZ API so I made my own through an implicit.
   *  
   *  At last, Validation[ResponseFunction, ResponseFuntion] is exposed as a ResponseFunction
   *  through another implicit conversion. It saves us the call to the Validation.fold() method
   */
  val plan = unfiltered.filter.Planify {
    authz.protect {
    case req @ Path(path) if path startsWith rm.basePath => {
      val baseURI = req.underlying.getRequestURL.toString
      val r: Resource = rm.resource(new URL(baseURI))
      req match {
        case GET(_) & Accept(accepts) if isHTML(accepts) => {
          val source = Source.fromFile("src/main/resources/skin.html")("UTF-8")
          val body = source.getLines.mkString("\n")
          Ok ~> ViaSPARQL ~> ContentType("text/html") ~> ResponseString(body)
        }
        case GET(_) | HEAD(_) =>
          for {
            model <- r.get() failMap { x => NotFound }
            encoding = RDFEncoding(req)
          } yield {
            req match {
              case GET(_) => Ok ~> ViaSPARQL ~> ContentType(encoding.toContentType) ~> ResponseModel(model, baseURI, encoding)
              case HEAD(_) => Ok ~> ViaSPARQL ~> ContentType(encoding.toContentType)
            }
          }
        case PUT(_) =>
          for {
            bodyModel <- modelFromInputStream(Body.stream(req), baseURI) failMap { t => BadRequest ~> ResponseString(t.getStackTraceString) }
            _ <- r.save(bodyModel) failMap { t => InternalServerError ~> ResponseString(t.getStackTraceString) }
          } yield Created
        case POST(_) => {
          Post.parse(Body.stream(req), baseURI) match {
            case PostUnknown => {
              logger.info("Couldn't parse the request")
              BadRequest ~> ResponseString("You MUST provide valid content for either: SPARQL UPDATE, SPARQL Query, RDF/XML, TURTLE")
            }
            case PostUpdate(update) => {
              logger.info("SPARQL UPDATE:\n" + update.toString())
              for {
                model <- r.get() failMap { t => NotFound }
                // TODO: we should handle an error here
                _ = UpdateAction.execute(update, model)
                _ <- r.save(model) failMap { t =>  InternalServerError ~> ResponseString(t.getStackTraceString)}
              } yield Ok
            }
            case PostRDF(diffModel) => {
              logger.info("RDF content:\n" + diffModel.toString())
              for {
                model <- r.get() failMap { t => NotFound }
                // TODO: we should handle an error here
                _ = model.add(diffModel)
                _ <- r.save(model) failMap { t =>  InternalServerError ~> ResponseString(t.getStackTraceString)}
              } yield Ok
            }
            case PostQuery(query) => {
              logger.info("SPARQL Query:\n" + query.toString())
              lazy val encoding = RDFEncoding(req)
              for {
                model <- r.get() failMap { t => NotFound }
              } yield {
                val qe: QueryExecution = QueryExecutionFactory.create(query, model)
                query.getQueryType match {
                  case SELECT =>
                    Ok ~> ContentType("application/sparql-results+xml") ~> ResponseResultSet(qe.execSelect())
                  case ASK =>
                    Ok ~> ContentType("application/sparql-results+xml") ~> ResponseResultSet(qe.execAsk())
                  case CONSTRUCT => {
                    val result: Model = qe.execConstruct()
                    Ok ~> ContentType(encoding.toContentType) ~> ResponseModel(model, baseURI, encoding)
                  }
                  case DESCRIBE => {
                    val result: Model = qe.execDescribe()
                    Ok ~> ContentType(encoding.toContentType) ~> ResponseModel(model, baseURI, encoding)
                  }
                }
              }
            }
          }
        }
        case _ => MethodNotAllowed ~> Allow("GET", "PUT", "POST")
      }
    }
  }

  }

}