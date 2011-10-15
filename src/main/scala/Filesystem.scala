package org.w3.readwriteweb

import org.w3.readwriteweb.util._

import java.io._
import java.net.URL
import org.slf4j.{Logger, LoggerFactory}
import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.shared.JenaException
import sys.error
import scalaz._
import Scalaz._

class Filesystem(
  baseDirectory: File,
  val basePath: String,
  val lang: String = "RDF/XML-ABBREV")(mode: RWWMode) extends ResourceManager {
  
  val logger:Logger = LoggerFactory.getLogger(this.getClass)

  def sanityCheck():Boolean = baseDirectory.exists

  def resource(url:URL):Resource = new Resource {
    val relativePath:String = url.getPath.replaceAll("^"+basePath.toString+"/?", "")
    val fileOnDisk = new File(baseDirectory, relativePath)

    private def createFileOnDisk():Unit = {
      // create parent directory if needed
      val parent = fileOnDisk.getParentFile
      if (! parent.exists) println(parent.mkdirs)
      val r = fileOnDisk.createNewFile()
      logger.debug("Create file %s with success: %s" format (fileOnDisk.getAbsolutePath, r.toString))
    }

    def get(): Validation[Throwable, Model] = {
      val model = ModelFactory.createDefaultModel()
      if (fileOnDisk.exists()) {
        val fis = new FileInputStream(fileOnDisk)
        try {
          val reader = model.getReader(lang)
          reader.read(model, fis, url.toString)
        } catch {
          case je:JenaException => error(je.toString)
        }
        fis.close()
        model.success
      } else {
        mode match {
          case AllResourcesAlreadyExist => model.success
          case ResourcesDontExistByDefault => new FileNotFoundException().fail
        }
      }
    }

    def save(model:Model):Validation[Throwable, Unit] =
      try {
        createFileOnDisk()
        val fos = new FileOutputStream(fileOnDisk)
        val writer = model.getWriter(lang)
        writer.write(model, fos, url.toString)
        fos.close().success
      } catch {
        case t => t.fail
      }

  }
  
}
