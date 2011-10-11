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
import org.w3.readwriteweb.{Resource}
import Query.{QueryTypeSelect => SELECT, QueryTypeAsk => ASK,
              QueryTypeConstruct => CONSTRUCT, QueryTypeDescribe => DESCRIBE}

import scalaz._
import Scalaz._

import org.w3.readwriteweb.util._
import java.security.KeyStore
import org.jsslutils.keystores.KeyStoreLoader
import org.jsslutils.sslcontext.{X509TrustManagerWrapper, X509SSLContextFactory}
import javax.net.ssl.{X509TrustManager, SSLContext}
import org.jsslutils.sslcontext.trustmanagers.TrustAllClientsWrappingTrustManager
import java.security.cert.X509Certificate
import scala.sys.SystemProperties
import collection.{mutable,immutable}
import webid.AuthFilter

class ReadWriteWeb(rm:ResourceManager) {
  
  val logger:Logger = LoggerFactory.getLogger(this.getClass)

  def isHTML(accepts:List[String]):Boolean = {
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
   *  this code makes use of the Validation monad. For example of how to use it, see
   *  http://scalaz.googlecode.com/svn/continuous/latest/browse.sxr/scalaz/example/ExampleValidation.scala.html
   *  
   *  the Resource abstraction returns Validation[Throwable, ?something]
   *  we use the for monadic constructs.
   *  Everything construct are mapped to Validation[ResponseFunction, ResponseFuntion],
   *  the left value always denoting the failure. Hence, the rest of the for-construct
   *  is not evaluated, but let the reader of the code understand clearly what's happening.
   *  
   *  This mapping is made possible with the failMap method. I couldn't find an equivalent
   *  in the ScalaZ API so I made my own through an implicit.
   *  
   *  At last, Validation[ResponseFunction, ResponseFuntion] is exposed as a ResponseFunction
   *  through another implicit conversion. It saves us the call to the Validation.lift() method
   */
  val read = unfiltered.filter.Planify {
    case req @ Path(path) if path startsWith rm.basePath => {
      val baseURI = req.underlying.getRequestURL.toString
      val r:Resource = rm.resource(new URL(baseURI))
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
                val qe:QueryExecution = QueryExecutionFactory.create(query, model)
                query.getQueryType match {
                  case SELECT =>
                    Ok ~> ContentType("application/sparql-results+xml") ~> ResponseResultSet(qe.execSelect())
                  case ASK =>
                    Ok ~> ContentType("application/sparql-results+xml") ~> ResponseResultSet(qe.execAsk())
                  case CONSTRUCT => {
                    val result:Model = qe.execConstruct()
                    Ok ~> ContentType(encoding.toContentType) ~> ResponseModel(model, baseURI, encoding)
                  }
                  case DESCRIBE => {
                    val result:Model = qe.execDescribe()
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

object Lookup {
  // a place to register services that can be looked up from anywhere.
  // this is very naive registration, compared to tools like Clerezza that use Apaches Felix's OSGI implementation

  private val db = new mutable.HashMap[Class[_],AnyRef]

  def get[T<:AnyRef](clzz :Class[T]) :Option[T] = db.get(clzz).map(e=>e.asInstanceOf[T])

  //http://stackoverflow.com/questions/3587286/how-does-scalas-2-8-manifest-work
  def put[T<:AnyRef : Manifest](obj: T): T = {
    def zref = manifest[T].erasure
    val ref: AnyRef = obj
    db.put(zref,ref).asInstanceOf[T]
  }

}

object ReadWriteWebMain {

  val logger:Logger = LoggerFactory.getLogger(this.getClass)


  // regular Java main
  def main(args: Array[String]) {
   
    val argsList = args.toList
    var httpPort: Int = 8080
    var httpsPort: Option[Int] = None
    var baseDir = new File(".")
    var baseUrl: String = "/"
    var relax = false
    
 
     def msg(err: String, exitCode: Int=1) = {
        println("ERROR:")
        println(err)
        println("""

example usage:
java -jar read-write-web.jar [-http 8080] [-https 8443] -dir ~/WWW/2011/09 -base /2011/09 [-strict|-relax]

Required:
  -dir $localpath :  the directory where the files are located
  -base $urlpath : the base url-path for those files

Options:
 -http $port  : set the http port to the port number, by default this will be port 8080
 -https $port : start the https server on the given port number
 -relax all resources potentially exist, meaning you get an empty RDF graph instead of a 404 (still experimental)
 -strict a GET on a resource will fail with a 404 (default mode if you omit it)

Properties:  (can be passed with -Dprop=value)

 * Keystore properties.
  jetty.ssl.keyStoreType : the type of the keystore, JKS by default usually
  jetty.ssl.keyStore=path : specify path to key store (for https server certificate)
  jetty.ssl.keyStorePassword=password : specify password for keystore store

 * Trust store
   Trust stores are not needed because we use the WebID protocol, and client certs are nearly never signed by CAs
 """)
        System.exit(exitCode)
        null
    }
    

    def parse(args: List[String]): Unit = {
      val res = args match {
        case "-https"::num::rest =>  { 
          httpsPort = Some(Integer.parseInt(num))
          rest
        }
        case "-http"::num::rest => {
          httpPort = num.toInt
          rest
        }
        case "-dir"::dir::rest => {
          baseDir = new File(dir)
          rest
        }
        case "-base"::path::rest=> {
          baseUrl=path
          rest
        }
        case "-strict"::rest=> {
          relax = false
          rest
        }
        case "-relax"::rest=> {
          relax = true
          rest
        }
        case something::other => msg("could not parse command: `"+something+"`",1)
        case Nil => return
      }
      parse(res)
    }
    
    parse(argsList)

    val mode =
      if (relax) {
        logger.info("info: using experimental relax mode")
        AllResourcesAlreadyExist
      } else {
        ResourcesDontExistByDefault
      }
    
    if (! baseDir.exists) {
      msg("%s does not exist" format (baseDir.getAbsolutePath),2)
    }

    val filesystem = new Filesystem(baseDir, baseUrl, lang="TURTLE")(mode)
    
    val app = new ReadWriteWeb(filesystem)

    val service = httpsPort match {
      case Some(port) => HttpsTrustAll(port,"0.0.0.0")
      case None => Http(httpPort)
    }

    val webCache = new WebCache()
    Lookup.put(webCache)

    // configures and launches a Jetty server
    service.filter {
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
    }.filter(new AuthFilter)
     .context("/public"){ ctx:ContextBuilder =>
      ctx.resources(MyResourceManager.fromClasspath("public/").toURI.toURL)
    }.filter(app.read).run()
    
  }

}

case class HttpsTrustAll(override val port: Int, override val host: String) extends Https(port, host) with TrustAll

trait TrustAll { self: Ssl =>
   import scala.sys.SystemProperties._

   lazy val sslContextFactory = new X509SSLContextFactory(
               serverCertKeyStore,
               tryProperty("jetty.ssl.keyStorePassword"),
               serverCertKeyStore); //this one is not needed since our wrapper ignores all trust managers

   lazy val trustWrapper = new X509TrustManagerWrapper {
     def wrapTrustManager(trustManager: X509TrustManager) = new TrustAllClientsWrappingTrustManager(trustManager)
   }

   lazy val serverCertKeyStore = {
      val keyStoreLoader = new KeyStoreLoader
   		keyStoreLoader.setKeyStoreType(System.getProperty("jetty.ssl.keyStoreType","JKS"))
   		keyStoreLoader.setKeyStorePath(trustStorePath)
   		keyStoreLoader.setKeyStorePassword(System.getProperty("jetty.ssl.keyStorePassword","password"))
      keyStoreLoader.loadKeyStore();
   }

   sslContextFactory.setTrustManagerWrapper(trustWrapper);


 	 lazy val trustStorePath =  new SystemProperties().get("jetty.ssl.keyStore") match {
       case Some(path) => path
       case None => new File(new File(tryProperty("user.home")), ".keystore").getAbsolutePath
   }

   sslConn.setSslContext(sslContextFactory.buildSSLContext())
   sslConn.setWantClientAuth(true)

}

