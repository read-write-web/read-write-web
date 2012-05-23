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

package org.w3.readwriteweb

/**
 * Image mime types adapted in a hurry from the Lang class.
 *
 * It really feels like oneshould have one whole set of mime type classes that cover all the cases.
 *
 * @author bblfish
 * @created 12/04/2012
 */

sealed trait Image {

  def suffix = this match {
    case JPEG => ".jpeg"
    case GIF => ".gif"
    case PNG => ".png"
  }

  def contentType : String

}


object Image {

  val supportedImages = Set(JPEG, GIF, PNG)

  def apply(contentType: String): Option[Image] = {
    contentType.trim.toLowerCase match {
      case "image/jpeg" => Some(JPEG)
      case "image/gif" => Some(GIF)
      case "image/png" => Some(PNG)
      case _ => None
    }
  }

  def apply(cts: Iterable[String]): Option[Image] =
    cts map Image.apply collectFirst {
      case Some(lang) => lang
    }
}

case object JPEG extends Image {
  val contentType = "image/jpeg"
}
case object GIF extends Image {
  val contentType = "image/gif"
}
case object PNG extends Image {
  val contentType = "image/png"
}
