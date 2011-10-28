package org.w3.readwriteweb

import java.io._
import java.net.URL

import org.slf4j.{Logger, LoggerFactory}

import com.hp.hpl.jena.rdf.model._
import com.hp.hpl.jena.shared.JenaException

import org.w3.readwriteweb.util._

import scalaz._
import Scalaz._
import java.io.StringWriter   // tbl
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
          case je:JenaException => error("Fail to parse <"+ url.toString +"> : "+ je)
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
        
        val temp = new StringWriter();  // tbl  kludge until Jena fixed @@@
        writer.write(model, temp, url.toString); //tbl 
        def baseBit(u:String) = u.slice(0, u.lastIndexOf('/')+1);  //tbl
        // We remove any base URIs on same server different port as well, for proxying.
        def generalize(u:String) = u.replaceAll("//localhost[:0-9]*/", "//localhost[:0-9]*/");
        // fos.write(("# Resource.scala 80: "+generalize(baseBit(url.toString))+"\n").getBytes) 
        logger.debug("@@ Munged output string with regexp: %s " format (generalize(baseBit(url.toString))));

        fos.write(temp.toString().replaceAll(generalize(baseBit(url.toString)), "").getBytes); // tbl
        
//        writer.write(model, fos, url.toString)

        fos.close().success
      } catch {
        case t => t.fail
      }

  }
  
}
