/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
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

package org.w3.readwriteweb.auth

import unfiltered.spec.netty.Started
import org.specs.Specification
import unfiltered.netty.{ReceivedMessage, ServerErrorResponse, cycle}
import org.w3.readwriteweb.auth.RDFAuthZ
import java.io.File
import org.w3.readwriteweb._
import grizzled.file.GrizzledFile._

import org.specs.specification.BeforeAfter

/**
 * @author hjs
 * @created: 24/10/2011
 */


trait SecureServed extends Started {
  import org.w3.readwriteweb.netty._

  def setup: (Https => Https)
  lazy val server = setup( KeyAuth_Https(port) )

}

/**
 * Netty resource managed with access control enabled
 */
trait SecureResourceManaged extends Specification with SecureServed {
  import org.jboss.netty.handler.codec.http._

  def resourceManager: ResourceManager

  val webCache = new WebCache()

  val rww = new cycle.Plan  with cycle.ThreadPool with ServerErrorResponse with ReadWriteWeb[ReceivedMessage,HttpResponse] {
    val rm = resourceManager
    def manif = manifest[ReceivedMessage]
    override val authz = new RDFAuthZ[ReceivedMessage,HttpResponse](webCache,resourceManager)
  }

  def setup = { _.plan(rww) }

}

trait SecureFileSystemBased extends SecureResourceManaged {
  lazy val mode: RWWMode = ResourcesDontExistByDefault

  lazy val lang = TURTLE

  lazy val baseURL = "/wiki"

  lazy val root = new File(new File(System.getProperty("java.io.tmpdir")), "readwriteweb")

  lazy val resourceManager = new Filesystem(root, baseURL, lang)(mode)

  doBeforeSpec {
    if (root.exists) root.deleteRecursively()
    root.mkdir()
  }

}
