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

package org.w3c.readwriteweb.cache


import com.ning.http.client.AsyncHandler.STATE

import java.net.URL
import org.w3.readwriteweb.Lang
import com.weiglewilczek.slf4s.Logging
import com.hp.hpl.jena.rdf.model.{ModelFactory, Model}
import scalaz.Zero
import java.util.Collections
import com.fasterxml.aalto.stax.InputFactoryImpl
import com.fasterxml.aalto.{AsyncXMLStreamReader, AsyncInputFeeder}
import com.hp.hpl.jena.rdf.arp.SAX2Model
import patch.AsyncJenaParser
import com.ning.http.client._


/**
 * Asynchronous URL fetcher.
 *
 * see http://www.cowtowncoder.com/blog/archives/2011/03/entry_451.html
 * for background on fasterxml's async parser
 *
 * @author bblfish
 * @created 27/01/2012
 */

class URLFetcher(url: URL) extends AsyncHandler[Model]()  with Logging {
  import scala.collection.JavaConverters._

//  var reader: RDFReader = _
//  var base: String = _

  var status: HttpResponseStatus = _
  var base: String = _
  var asyncParser: AsyncJenaParser = _

  lazy val asyncReader: AsyncXMLStreamReader = new InputFactoryImpl().createAsyncXMLStreamReader();
  lazy val feeder: AsyncInputFeeder = asyncReader.getInputFeeder();
  lazy val model: Model = ModelFactory.createDefaultModel()
  
  def onThrowable(t: Throwable) {
    logger.error(t.getMessage)
  }

  def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
    val bytes = bodyPart.getBodyPartBytes
    if (feeder.needMoreInput()) {
      feeder.feedInput(bytes,0,bytes.length)
    } else logger.warn("feeder does not want more input - parsing did not consume all")

    //should one check if asyncParser needs more input?
    asyncParser.parse()

    STATE.CONTINUE
  }

  def onStatusReceived(responseStatus: HttpResponseStatus) = {
    status = responseStatus
    STATE.CONTINUE
  }

  def onHeadersReceived(headers: HttpResponseHeaders) = {
    if (status.getStatusCode < 200 && status.getStatusCode > 204) {
      STATE.CONTINUE
    } else {
      val typeHdr =   nullSquisher( headers.getHeaders.get("Content-Type") ).asScala
      logger.info("Content-Types ➤ " + typeHdr.mkString(" ➤ "))
      val mime = typeHdr.flatMap(mime => Lang(mime.split(";")(0))).headOption

      val locHdr = nullSquisher ( headers.getHeaders.get("Content-Location")).asScala
      logger.info("Content-Location ➤ " + locHdr.mkString(" ➤ "))
      val location = locHdr.headOption match {
        case Some(loc) => new URL(url, loc)
        case None => new URL(url.getProtocol, url.getAuthority, url.getPort, url.getPath)
      }
      base = location.toString
      
 // currently we assume rdf/xml
 //     val lang = mime getOrElse Lang.default
 //     reader = model.getReader(lang.jenaLang)

      asyncParser = new AsyncJenaParser(SAX2Model.create(base, model),asyncReader)

      STATE.CONTINUE
    }
  }

  def onCompleted() = {
    feeder.endOfInput()
    asyncReader.close()
    model
  }


  def nullSquisher[T](body: => T)(implicit z: Zero[T]): T =
    try {
      val res = body;
      if (res == null) z.zero else res
    } catch {
      case e => {
        logger.warn("squished an exception to null",e)
        z.zero
      }
    }
   implicit def JavaListZero[A]: Zero[java.util.List[A]] = new Zero[java.util.List[A]] { val zero = Collections.emptyList[A]() }
}

/**
 * test object to run from the scala command line
 */
object ModelCache  {
  lazy val url = "http://bblfish.net/people/henry/card.rdf"
  //for debugging
  lazy val config = new AsyncHttpClientConfig.Builder().
    setConnectionTimeoutInMs(60000*15).
    setIdleConnectionTimeoutInMs(60000*15).
    setRequestTimeoutInMs(60000*15).
    setWebSocketIdleTimeoutInMs(60000*15).build()
  lazy val client = new AsyncHttpClient(config)

  def response(url: String) = client.prepareGet(url).
    setFollowRedirects(true).
    execute(new URLFetcher(new URL(url)))

}
