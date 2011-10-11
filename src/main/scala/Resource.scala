package org.w3.readwriteweb

import java.io._
import java.net.URL

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.shared.JenaException

import scalaz._
import Scalaz._

import _root_.scala.sys.error

trait ResourceManager {
  def basePath:String
  def sanityCheck():Boolean
  def resource(url:URL):Resource
}

trait Resource {
  def get():Validation[Throwable, Model]
  def save(model:Model):Validation[Throwable, Unit]
}

class Filesystem(baseDirectory:File, val basePath:String, val lang:String = "RDF/XML-ABBREV")(implicit mode:RWWMode) extends ResourceManager {
  
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
    
    def get():Validation[Throwable, Model] = {
      val m = ModelFactory.createDefaultModel()
      if (fileOnDisk.exists()) {
        val fis = new FileInputStream(fileOnDisk)
        try {
          m.read(fis, url.toString)
        } catch {
          case je:JenaException => error("File %s was either empty or corrupted: considered as empty graph" format fileOnDisk.getAbsolutePath)
        }
        fis.close()
        m.success
      } else {
        mode match {
          case AllResourcesAlreadyExist => m.success
          case ResourcesDontExistByDefault => new FileNotFoundException().fail
      }
      }
    }
    
    def save(model:Model):Validation[Throwable, Unit] =
      try {
        createFileOnDisk()
        val fos = new FileOutputStream(fileOnDisk)
        val writer = model.getWriter("RDF/XML-ABBREV")
        writer.write(model, fos, url.toString)
        fos.close().success
      } catch {
        case t => t.fail
      }

  }
  
}

