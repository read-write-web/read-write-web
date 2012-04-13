package org.w3.readwriteweb

import org.w3.readwriteweb.util._

import java.net.URL
import com.hp.hpl.jena.rdf.model._
import scalaz.{Resource => _, _}
import Scalaz._
import java.io.InputStream

trait ResourceManager {
  def basePath:String
  def sanityCheck():Boolean
  def resource(url:URL):Resource
}

object CacheControl extends Enumeration {
  val CacheOnly, CacheFirst, NoCache = Value
}

trait Resource {
  def name: URL

  /**
   * get the resource as a model.
   * Note: the cache policy only really makes sense for remote resources, not for the file system.
   * Note: returning a model only makes sense when the resource is something that can be transformed to
   *       one which is not always the case, especially not for images. (see below)
   * @param policy
   * @return
   */
  def get(policy: CacheControl.Value = CacheControl.CacheFirst): Validation[Throwable, Model]

  /**
   * Many resources are not images. We need to get inputStreams for this.
   * (this model here is getting more and more complicated. The get that returns a model above cannot
   * simply be reduced to this one, as in the FileResource doing a GET on the directory should return
   * a Graph describing the directory for example. Dealing with this is going to be a bit more tricky than
   * I (bblfish) have time for at this point - as it would probably require quite a deep rewrite.)
   * )
   * @return
   */
  def getStream: Validation[Throwable,InputStream]
  def delete: Validation[Throwable, Unit]
  def save(model:Model):Validation[Throwable, Unit]

  /**
   * PUT the inputstream in the location
   *
   * same comments as with getStream
   *
   * @param in inputstream containing serialisation
   */
  def putStream(in: InputStream): Validation[Throwable, Unit]

  //These two methods only work when called on directories
  def createDirectory: Validation[Throwable, Unit]
  def create(): Validation[Throwable, Resource]
}

