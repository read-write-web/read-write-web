/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * under the MIT licence defined
 *    http://www.opensource.org/licenses/mit-license.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.w3.readwriteweb.netty


import _root_.auth.WebIDSrvc
import org.clapper.argot.ArgotUsageException
import scala.Console._
import org.w3.readwriteweb.auth.{X509view, RDFAuthZ}
import org.w3.readwriteweb._
import org.jboss.netty.handler.codec.http.HttpResponse
import unfiltered.request.Path
import java.io.{InputStream, OutputStream}
import unfiltered.response._
import unfiltered.netty._
import collection.immutable.List
import scala.None

/**
 * ReadWrite Web for Netty server, allowing TLS renegotiation
 *
 * @author hjs
 * @created: 21/10/2011
 */

object ReadWriteWebNetty extends ReadWriteWebArgs {

  // regular Java main
   def main(args: Array[String]) {

     try {
       parser.parse(args)
       postParse
     } catch {
       case e: ArgotUsageException => err.println(e.message); sys.exit(1)
     }
 
     val filesystem =
       new Filesystem(
         rootDirectory.value.get,
         baseURL.value.get,
         lang=rdfLanguage.value getOrElse RDFXML)(mode.value getOrElse ResourcesDontExistByDefault)
     
     val rww = new cycle.Plan  with cycle.ThreadPool with ServerErrorResponse with ReadWriteWeb[ReceivedMessage,HttpResponse]{
          val rm = filesystem
          def manif = manifest[ReceivedMessage]
          override val authz = new RDFAuthZ[ReceivedMessage,HttpResponse](filesystem)
     }

     val echo =  new cycle.Plan  with cycle.ThreadPool with ServerErrorResponse with NettyEchoPlan

     val domain = host.value.getOrElse("0.0.0.0")
     //only one of the following two servers can be started at one time
     //having two would require a lot more care with issues of overwriting
     httpsPort.value.map {
       port => new KeyAuth_Https(port).
         plan(publicStatic).
         plan( echo ).
         plan( x509v ).
         plan( webidp ).
         plan( rww ).run()
     }

     httpPort.value.map {
      port => Http(httpPort.value.get,domain).
        plan(publicStatic).
        plan( echo ).
        plan( rww ).run()
    }
     
   }



  trait StaticFiles extends PartialFunction[String, ResponseFunction[Any]] {
    /* override this if the local path is somehow different from the url path */
    def toLocal(webpath: String): String = webpath
    val extension = "([^\\.]*?)$".r
    val extList: List[String] = List("css", "png")

    private def toString(in: InputStream): String = {
      val source = scala.io.Source.fromInputStream(in)
      val lines = source.mkString
      source.close()
      lines
    }

    def isDefinedAt(path: String): Boolean = try {
      val in = classOf[StaticFiles].getResourceAsStream(toLocal(path))
      (in != null) & (extension.findFirstIn(path).exists(extList contains _))
    } catch {
      case _ => false
    }

    def apply(path: String): ResponseFunction[Any] = {
      try {
        val in = classOf[StaticFiles].getResourceAsStream(toLocal(path))
        extension.findFirstIn(path).getOrElse("css") match {
          case "css" => Ok ~> ResponseString(toString(in)) ~> CssContent
          case "js" => Ok ~> ResponseString(toString(in)) ~> JsContent
          case "png" => Ok ~> ResponseBin(in) ~> ContentType("image/png")
        }
      } catch {
        case _ => NotFound
      }
    }
  }

  object publicStatic  extends  cycle.Plan  with cycle.ThreadPool with ServerErrorResponse with StaticFiles {
    val initialPath= "/public"

    def intent = {
      case Path(path) if path.startsWith(initialPath) => apply(path)
    }
  }

  object x509v extends  cycle.Plan  with cycle.ThreadPool with ServerErrorResponse with X509view[ReceivedMessage,HttpResponse] {
    def manif = manifest[ReceivedMessage]
  }
  
  object webidp extends cycle.Plan with cycle.ThreadPool with ServerErrorResponse with WebIDSrvc[ReceivedMessage, HttpResponse] {
    def manif = manifest[ReceivedMessage]
    val signer = ReadWriteWebNetty.signer
  }

  /**
   * set environmental variables, but adapt them for the underlying server
   * (a silly hack because jetty and netty in unfiltered use different TLS security
   * properties, and I don't want to dig through each library to set that right just now)
   * @param name
   * @param value
   */
  def setPropHack(name: String, value: String) {
    System.setProperty("netty."+name,value)
  }
}


case class ResponseBin(bis: InputStream) extends ResponseStreamer {
  override def stream(out: OutputStream) {
    val buf = new Array[Byte](4096)
    Iterator continually bis.read(buf) takeWhile (-1 !=) foreach  { bytesRead =>
      out.write(buf,0,bytesRead)
    }
  }
}