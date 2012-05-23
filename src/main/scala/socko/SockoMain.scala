/*
 * Copyright (c) 2012 Henry Story (bblfish.net)
 * under the MIT licence defined at
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

package socko

import org.mashupbots.socko.utils.Logger
import org.mashupbots.socko.webserver.{WebServerConfig, WebServer}
import akka.actor.{Actor, Props, ActorSystem}
import org.mashupbots.socko.context.HttpRequestProcessingContext
import java.util.Date
import org.mashupbots.socko.routes.{GET, Method, Routes}

/**
* @author bblfish
* @created 18/05/2012
*/


object SockoApp extends Logger {
  //
  // STEP #1 - Define Actors and Start Akka
  // See `HelloProcessor`
  //
  val actorSystem = ActorSystem("HelloExampleActorSystem")

  //
  // STEP #2 - Define Routes
  //
  val routes = Routes({
    case GET(request) => {
      actorSystem.actorOf(Props[HelloProcessor]) ! request
    }
  })

  //
  // STEP #3 - Start and Stop Socko Web Server
  //
  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(), routes)
    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })

    System.out.println("Open your browser and navigate to http://localhost:8888")
  }
}

class HelloProcessor extends Actor {
  def receive = {
    case request: HttpRequestProcessingContext =>
      request.writeResponse("Hello from Socko (" + new Date().toString + ")")
      context.stop(self)
  }
}

