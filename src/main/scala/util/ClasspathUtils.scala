package org.w3.readwriteweb.util

import java.io.{File, FileWriter}
import java.util.jar._
import scala.collection.JavaConversions._
import scala.io.Source
import java.net.{URL, URLDecoder}
import org.slf4j.{Logger, LoggerFactory}

/** useful stuff to read resources from the classpath */
object ClasspathUtils {
  
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val clazz: Class[_] = this.getClass
  val classloader = this.getClass.getClassLoader
  
  /** http://www.uofr.net/~greg/java/get-resource-listing.html
   */
  def getResourceListing(path: String): List[String] = {
    var dirURL: URL = classloader.getResource(path)
    if (dirURL != null && dirURL.getProtocol == "file") {
      /* A file path: easy enough */
      new File(dirURL.toURI).list.toList
    } else {
      if (dirURL == null) {
        val me = clazz.getName().replace(".", "/")+".class"
        dirURL = classloader.getResource(me)
      }
      if (dirURL.getProtocol == "jar") {
        val jarPath = dirURL.getPath.substring(5, dirURL.getPath().indexOf("!"))
        val jar: JarFile = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))
        val entries = jar.entries filter { _.getName startsWith path } map { e => {
          var entry = e.getName substring path.length
          val checkSubdir = entry indexOf "/"
          if (checkSubdir >= 0) entry = entry.substring(0, checkSubdir)
          entry
        } }
        entries filterNot { _.isEmpty } toList
      } else
        sys.error("Cannot list files for URL "+dirURL);
    }
  }
  
  /** extract a path found in the classpath
   * 
   *  @return the file on disk
   */
  def fromClasspath(path: String, base: File = new File("src/main/resources")): File = {
    val workingPath = new File(base, path)
    if (workingPath isDirectory) {
      workingPath
    } else {
      val dir = new File(System.getProperty("java.io.tmpdir"),
                         "virtual-trainer-" + scala.util.Random.nextInt(10000).toString)
      if (! dir.mkdir()) logger.error("Couldn't extract %s from jar to %s" format (path, dir.getAbsolutePath))
      val entries = getResourceListing(path) foreach { entry => {
        val url = classloader.getResource(path + entry)
        val content = Source.fromURL(url, "UTF-8").getLines.mkString("\n")
        val writer = new FileWriter(new File(dir, entry))
        writer.write(content)
        writer.close()
      }
    }
    dir
    }
  }
  
}
