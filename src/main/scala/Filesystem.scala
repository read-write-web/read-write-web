package org.w3.readwriteweb

import java.io._
import java.net.URL
import org.slf4j.{Logger, LoggerFactory}
import com.hp.hpl.jena.rdf.model.{Resource => _, _}
import com.hp.hpl.jena.shared.JenaException

import scalaz.{Resource => _, _}
import Scalaz._

import scala.sys

import com.hp.hpl.jena.vocabulary.RDF
import org.w3.readwriteweb.Image

class Filesystem(
  baseDirectory: File,
  val basePath: String,
  val lang: Lang)(mode: RWWMode) extends ResourceManager {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def sanityCheck(): Boolean =
    baseDirectory.exists && baseDirectory.isDirectory

  def resource(url: URL): Resource = new Resource {
    def name() = url

    val relativePath: String = url.getPath.replaceAll("^" + basePath.toString + "/?", "")
    val fileOnDisk = new File(baseDirectory, relativePath)
    lazy val parent = fileOnDisk.getParentFile

    private def parentMustExist(): Unit = {
      val parent = fileOnDisk.getParentFile
      if (!parent.exists) sys.error("Parent directory %s does not exist" format parent.getAbsolutePath)
      if (!parent.isDirectory) sys.error("Parent %s is not a directory" format parent.getAbsolutePath)
    }

    private def createDirectoryOnDisk(): Unit = {
      parentMustExist()
      val r = fileOnDisk.mkdir()
      if (!r) sys.error("Could not create %s" format fileOnDisk.getAbsolutePath)
      logger.debug("%s successfully created: %s" format(fileOnDisk.getAbsolutePath, r.toString))
    }

    private def createFileOnDisk(): Unit = {
      parentMustExist()
      val r = fileOnDisk.createNewFile()
      logger.debug("%s successfully created: %s" format(fileOnDisk.getAbsolutePath, r.toString))
    }

    def get(unused: CacheControl.Value = CacheControl.CacheFirst): Validation[Throwable, Model] = {
      val model = ModelFactory.createDefaultModel()

      //for files: other possible ontologies to use would be
      // "Linked Data Basic Profile 1.0" http://www.w3.org/Submission/2012/02/
      // "the posix ontology" used by data.fm http://www.w3.org/ns/posix/stat#
      if (fileOnDisk.isDirectory) {
        val sioc = "http://rdfs.org/sioc/ns#"
        val dirRes = model.createResource(name.toString)
        dirRes.addProperty(RDF.`type`, model.createResource(sioc + "Container"))
        for (f <- fileOnDisk.listFiles()) {
          val furl = new URL(name, f.getName)
          val item = model.createResource(furl.toString)
          dirRes.addProperty(model.createProperty(sioc + "container_of"), item)
          if (f.isDirectory) item.addProperty(RDF.`type`, model.createResource(sioc + "Container"))
          else item.addProperty(RDF.`type`, model.createResource(sioc + "Item"))
        }
        model.success
      } else {
        val guessLang = fileOnDisk.getName match {
          case Authoritative.r(_, suffix) => Representation.fromSuffix(suffix) match {
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
    }

    def getStream = try {
      new BufferedInputStream(new FileInputStream(fileOnDisk)).success
    } catch {
      case fe: FileNotFoundException => fe.fail
      case se: SecurityException => se.fail
    }

    def putStream(in: InputStream): Validation[Throwable, Unit] = {
      val out = new FileOutputStream(fileOnDisk)
      val buf = new Array[Byte](4096)
      try {
       val good = Iterator continually in.read(buf) takeWhile (-1 !=) foreach  { bytesRead =>
          out.write(buf,0,bytesRead)
        }
        good.success
      } catch {
        case ioe: IOException => ioe.fail
      }

    }


    def save(model: Model): Validation[Throwable, Unit] =
      try {
        parent.mkdirs()
        val fos = new FileOutputStream(fileOnDisk)
        val writer = model.getWriter(lang.jenaLang)
        writer.write(model, fos, url.toString)
        fos.close().success
      } catch {
        case t => t.fail
      }

    def createDirectory: Validation[Throwable, Unit] =
      try {
        createDirectoryOnDisk().success
//        val fos = new FileOutputStream(fileOnDisk)
//        val writer = model.getWriter(lang.contentType)
//        writer.write(model, fos, url.toString)
//        fos.close().success
      } catch {
        case t => t.fail
      }

    def delete: Validation[Throwable, Unit] = try {
      if (fileOnDisk.delete()) ().success
      else new Exception("Failed to delete file "+fileOnDisk).fail
    } catch {
      case e: SecurityException => e.fail
    }

    def create(contentType: Representation): Validation[Throwable, Resource] = {
      if (!fileOnDisk.exists())
        new Throwable("Must first create " + name()).fail
      else if (!fileOnDisk.isDirectory)
        new Throwable("Can only create a resource in a directory/collection which this is not " + name()).fail
      else try {
        //todo: the class hierarchy of content types needs to be improved.
        val suffix = contentType match {
          case RDFRepr(lang) => lang.suffix
          case ImageRepr(tpe) => tpe.suffix
          case l => lang.suffix
        }
        val path = File.createTempFile("res", suffix, fileOnDisk)
        resource(new URL(name(), path.getName)).success
      } catch {
        case ioe: IOException => ioe.fail
      }
    }

  }

}
  

