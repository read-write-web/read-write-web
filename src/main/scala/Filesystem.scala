package org.w3.readwriteweb

import java.io._
import java.net.URL
import org.slf4j.{Logger, LoggerFactory}
import com.hp.hpl.jena.rdf.model.{Resource => _, _}
import com.hp.hpl.jena.shared.JenaException

import scalaz.{Resource => _, _}
import Scalaz._

import scala.sys

class Filesystem(
  baseDirectory: File,
  val basePath: String,
  val lang: Lang)(mode: RWWMode) extends ResourceManager {
  
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  
  def sanityCheck(): Boolean =
    baseDirectory.exists && baseDirectory.isDirectory

  def resource(url: URL): Resource = new Resource {
    val relativePath: String = url.getPath.replaceAll("^"+basePath.toString+"/?", "")
    val fileOnDisk = new File(baseDirectory, relativePath)
    
    private def parentMustExist(): Unit = {
      val parent = fileOnDisk.getParentFile
      if (! parent.exists) sys.error("Parent directory %s does not exist" format parent.getAbsolutePath)
      if (! parent.isDirectory) sys.error("Parent %s is not a directory" format parent.getAbsolutePath)
    }
    
    private def createDirectoryOnDisk(): Unit = {
      parentMustExist()
      val r = fileOnDisk.mkdir()
      if (!r) sys.error("Could not create %s" format fileOnDisk.getAbsolutePath)
      logger.debug("%s successfully created: %s" format (fileOnDisk.getAbsolutePath, r.toString))
    }
    
    private def createFileOnDisk(): Unit = {
      parentMustExist()
      val r = fileOnDisk.createNewFile()
      logger.debug("%s successfully created: %s" format (fileOnDisk.getAbsolutePath, r.toString))
    }
    
    def get(): Validation[Throwable, Model] = {
      val model = ModelFactory.createDefaultModel()
      val guessLang = fileOnDisk.getName match {
        case Authoritative.r(_,suffix) => Representation.fromSuffix(suffix) match {
          case RDFRepr(rdfLang) => rdfLang
          case _ => lang
        }
        case _ => lang
      }
      if (fileOnDisk.exists()) {
        val fis = new FileInputStream(fileOnDisk)
        try {
          val reader = model.getReader(guessLang.jenaLang)
          reader.read(model, fis, url.toString)
        } catch {
          case je: JenaException => throw je
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
    
    def save(model: Model): Validation[Throwable, Unit] =
      try {
        createFileOnDisk()
        val fos = new FileOutputStream(fileOnDisk)
        val writer = model.getWriter(lang.jenaLang)
        writer.write(model, fos, url.toString)
        fos.close().success
      } catch {
        case t => t.fail
      }

    def createDirectory(model: Model): Validation[Throwable, Unit] =
      try {
        createDirectoryOnDisk().success
//        val fos = new FileOutputStream(fileOnDisk)
//        val writer = model.getWriter(lang.contentType)
//        writer.write(model, fos, url.toString)
//        fos.close().success
      } catch {
        case t => t.fail
      }

  }
  
}
