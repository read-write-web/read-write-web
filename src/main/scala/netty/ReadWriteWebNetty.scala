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

import org.clapper.argot.ArgotUsageException
import scala.Console._
import org.w3.readwriteweb.auth.{X509view, RDFAuthZ}
import org.w3.readwriteweb._
import org.jboss.netty.handler.codec.http.HttpResponse
import unfiltered.jetty.ContextBuilder
import util.ClasspathUtils
import unfiltered.netty.{ServerErrorResponse, ReceivedMessage, cycle}
import unfiltered.request.Path
import java.io.{FileInputStream, File}
import unfiltered.response.{JsContent, NotFound, ResponseString, Ok}

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
          override val authz = new RDFAuthZ[ReceivedMessage,HttpResponse](webCache,filesystem)
     }

     //this is incomplete: we should be able to start both ports.... not sure how to do this yet.
     val service = httpsPort.value match {
       case Some(port) => new KeyAuth_Https(port)
       case None => new KeyAuth_Https(httpPort.value.get)
     }

     // configures and launches a Netty server
     service.plan(publicStatic).
       plan( x509v ).
       plan( rww ).run()
     
   }

  object publicStatic extends  cycle.Plan  with cycle.ThreadPool with ServerErrorResponse {
    def intent = {
      case Path(path) if path.startsWith("/public") => {
        try {
          val in = publicStatic.getClass.getResourceAsStream(path)
          val source = scala.io.Source.fromInputStream(in)
          val lines = source.mkString
          source.close()
          Ok ~> ResponseString(lines) ~> JsContent //currently that's all I am interested in, but of course this is not right.
        } catch {
          case _ => NotFound
        }
      }
    }
  }

  object x509v extends  cycle.Plan  with cycle.ThreadPool with ServerErrorResponse with X509view[ReceivedMessage,HttpResponse] {
    def wc = webCache
    def manif = manifest[ReceivedMessage]
  }

}

