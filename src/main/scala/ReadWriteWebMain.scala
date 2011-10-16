package org.w3.readwriteweb

import org.w3.readwriteweb.util._

import javax.servlet._
import javax.servlet.http._
import unfiltered.jetty._
import java.io.File
import Console.err
import org.slf4j.{Logger, LoggerFactory}

import org.clapper.argot._
import ArgotConverters._

object ReadWriteWebMain {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val parser = new ArgotParser("read-write-web")

  val mode = parser.option[RWWMode](List("mode"), "m", "wiki mode") {
    (sValue, opt) =>
      sValue match {
        case "wiki" => AllResourcesAlreadyExist
        case "strict" => ResourcesDontExistByDefault
        case _ => throw new ArgotConversionException("Option %s: must be either wiki or strict" format (opt.name, sValue))
      }
    }

  val rdfLanguage = parser.option[Lang](List("language"), "l", "RDF language") {
    (sValue, opt) =>
      sValue match {
        case "n3" => N3
        case "turtle" => TURTLE
        case "rdfxml" => RDFXML
        case _ => throw new ArgotConversionException("Option %s: must be either n3, turtle or rdfxml" format (opt.name, sValue))
      }
  }

  val port = parser.parameter[Int]("port", "Port to use", false)

  val rootDirectory = parser.parameter[File]("rootDirectory", "root directory", false) {
    (sValue, opt) => {
      val file = new File(sValue)
      if (! file.exists)
        throw new ArgotConversionException("Option %s: %s must be a valid path" format (opt.name, sValue))
      else
        file
    }
  }

  val baseURL = parser.parameter[String]("baseURL", "base URL", false)

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
    
    val app = new ReadWriteWeb(filesystem)

    // configures and launches a Jetty server
    unfiltered.jetty.Http(port.value.get)
    .filter(new FilterLogger(logger))
    .context("/public") {
      ctx: ContextBuilder =>
        ctx.resources(ClasspathUtils.fromClasspath("public/").toURI.toURL)
    }.filter(app.plan).run()
    
  }

}

