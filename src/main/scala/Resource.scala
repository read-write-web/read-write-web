package org.w3.readwriteweb

import java.io._
import java.net.URL

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.shared.JenaException

import org.w3.readwriteweb.util._

trait ResourceManager {
  def sanityCheck():Boolean
  def resource(url:URL):Resource
}
trait Resource {
  def get():Model
  def save(model:Model):Unit
}

class Filesystem(baseDirectory:File, basePath:String, val lang:String = "RDF/XML-ABBREV")(implicit mode:RWWMode) extends ResourceManager {
  
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
    
    def get():Model = {
      val m = ModelFactory.createDefaultModel()
      if (fileOnDisk.exists()) {
        val fis = new FileInputStream(fileOnDisk)
        try {
          m.read(fis, url.toString)
        } catch {
          case je:JenaException => sys.error("File %s was either empty or corrupted: considered as empty graph" format fileOnDisk.getAbsolutePath)
        }
        fis.close()
        m
      } else {
        mode match {
          case AllResourcesAlreadyExist => m
          case ResourcesDontExistByDefault => throw new FileNotFoundException
      }
      }
    }
    
    def save(model:Model):Unit = {
      createFileOnDisk()
      val fos = new FileOutputStream(fileOnDisk)
      val writer = model.getWriter("RDF/XML-ABBREV")
      writer.write(model, fos, url.toString)
      fos.close()
    }
    
  }
  
}
