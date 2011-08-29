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
  def getAndCreateIfDoesNotExist():Model
  def save(model:Model):Unit
}

class Filesystem(baseDirectory:File, basePath:String, val lang:String = "RDF/XML-ABBREV") extends ResourceManager {
  
  val logger:Logger = LoggerFactory.getLogger(this.getClass)
  
  def sanityCheck():Boolean = baseDirectory.exists
  
  def resource(url:URL):Resource = new Resource {
    val relativePath:String = url.getPath.replaceAll("^"+basePath.toString, "")
    val fileOnDisk = new File(baseDirectory, relativePath)
    
    def get():Model = {
      val fis = new FileInputStream(fileOnDisk)
      val m = ModelFactory.createDefaultModel()
      try {
        m.read(fis, url.toString)
      } catch {
        case je:JenaException => logger.error("File %s was either empty or corrupted: considered as empty graph" format fileOnDisk.getAbsolutePath)
      }
      fis.close()
      m
    }
    
    def getAndCreateIfDoesNotExist():Model = {
      val model = ModelFactory.createDefaultModel()
      if (fileOnDisk exists) {
        val fis = new FileInputStream(fileOnDisk)
        model.read(fis, url.toString, lang)
        fis.close()
      }
      // if file does not exist, create it
      if (! fileOnDisk.exists) {
        // create parent directory if needed
        val parent = fileOnDisk.getParentFile
        if (! parent.exists) println(parent.mkdirs)
        val r = fileOnDisk.createNewFile()
        logger.debug("Create file %s with success: %s" format 
            (fileOnDisk.getAbsolutePath, r.toString))
      }
      model
    }
    
    def save(model:Model):Unit = {
      val fos = new FileOutputStream(fileOnDisk)
      val writer = model.getWriter("RDF/XML-ABBREV")
      writer.write(model, fos, url.toString)
      fos.close()
    }
    
  }
  
}
