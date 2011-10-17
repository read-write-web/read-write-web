package org.w3.readwriteweb

import auth.{AuthZ, NullAuthZ}
import org.w3.readwriteweb.util._

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
import unfiltered.filter.Plan
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import unfiltered.request._
import unfiltered.response._

//object ReadWriteWeb {
//
//  val defaultHandler: PartialFunction[Throwable, HttpResponse[_]] = {
//    case t => InternalServerError ~> ResponseString(t.getStackTraceString)
//  }
//
//}

class ReadWriteWeb(rm: ResourceManager, implicit val authz: AuthZ = NullAuthZ)  {
  // a few type short cuts to make it easier to reason with the code here
  // one may want to generalise this code so that it does not depend so strongly on servlets.
  type Req = HttpRequest[HttpServletRequest]
  type Res = ResponseFunction[HttpServletResponse]

  val logger: Logger = LoggerFactory.getLogger(this.getClass)


 /**
  *  The Servlet Filter for the ReadWriteWeb
  *
  *  Planify.apply takes an Intent, which is defined in Cycle by
  *  type Intent [-A, -B] = PartialFunction[HttpRequest[A], ResponseFunction[B]]
  *  the corresponding syntax is: case ... => ...
  */
  //bblfish: I don't understand why this has to be lazy, but if it is not, it the plan ends up throwing a null
  //pointer exception.
  lazy val plan = unfiltered.filter.Planify(intent)

  /**
   * The partial function that if triggered sends to the readwrite web code.
   * It wraps the ReadWriteWeb function with the AuthZ passed in the argument
   * ( Note that we don't want to protect this intent, since that would be to apply the security to all other applications,
   * many of which may want different authorization implementations )
   */
  val intent : Plan.Intent = {
      case req @ Path(path) if path startsWith rm.basePath => authz.protect(readWriteWebIntent)(req)
  }

  /**
   * The core ReadWrite web function
   * ( This is not a partial function and so is not a Plan.Intent )
   *
   *  This code makes use of ScalaZ Validation. For example of how to use it, see
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
  val readWriteWebIntent =  (req: Req) => {

          val Authoritative(uri, representation) = req
          val r: Resource = rm.resource(uri)
          val res: Res = req match {
            case GET(_) if representation == HTMLRepr => {
              val source = Source.fromFile("src/main/resources/skin.html")("UTF-8")
              val body = source.getLines.mkString("\n")
              Ok ~> ViaSPARQL ~> ContentType("text/html") ~> ResponseString(body)
            }
            case GET(_) | HEAD(_) =>
              for {
                model <- r.get() failMap { x => NotFound }
                lang = representation match {
                  case RDFRepr(l) => l
                  case _ => Lang.default
                }
              } yield {
                req match {
                  case GET(_) => Ok ~> ViaSPARQL ~> ContentType(lang.contentType) ~> ResponseModel(model, uri, lang)
                  case HEAD(_) => Ok ~> ViaSPARQL ~> ContentType(lang.contentType)
                }
              }
            case PUT(_) & RequestLang(lang) if representation == DirectoryRepr => {
              for {
                bodyModel <- modelFromInputStream(Body.stream(req), uri, lang) failMap { t => BadRequest ~> ResponseString(t.getStackTraceString) }
                _ <- r.createDirectory(bodyModel) failMap { t => InternalServerError ~> ResponseString(t.getStackTraceString) }
              } yield Created
            }
            case PUT(_) & RequestLang(lang) =>
              for {
                bodyModel <- modelFromInputStream(Body.stream(req), uri, lang) failMap { t => BadRequest ~> ResponseString(t.getStackTraceString) }
                _ <- r.save(bodyModel) failMap { t => InternalServerError ~> ResponseString(t.getStackTraceString) }
              } yield Created
            case PUT(_) =>
              BadRequest ~> ResponseString("Content-Type MUST be one of: " + Lang.supportedAsString)
            case POST(_) & RequestContentType(ct) if Post.supportContentTypes contains ct => {
              Post.parse(Body.stream(req), uri, ct) match {
                case PostUnknown => {
                  logger.info("Couldn't parse the request")
                  BadRequest ~> ResponseString("You MUST provide valid content for given Content-Type: " + ct)
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
                  lazy val lang = RequestLang(req) getOrElse Lang.default
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
                        Ok ~> ContentType(lang.contentType) ~> ResponseModel(model, uri, lang)
                      }
                      case DESCRIBE => {
                        val result: Model = qe.execDescribe()
                        Ok ~> ContentType(lang.contentType) ~> ResponseModel(model, uri, lang)
                      }
                    }
                  }
                }
              }
            }
            case POST(_) =>
              BadRequest ~> ResponseString("Content-Type MUST be one of: " + Post.supportedAsString)
            case _ => MethodNotAllowed ~> Allow("GET", "PUT", "POST")
          }
          res
        }
      
    
    
  

}