/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * under the MIT licence defined at
 *    http://www.opensource.org/licenses/mit-license.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package sommer

import com.hp.hpl.jena.vocabulary.RDF
import com.hp.hpl.jena.sparql.vocabulary.FOAF
import java.lang.String
import org.w3.readwriteweb.{Resource, WebCache}
import scalaz.Validation
import java.net.URL
import collection._
import collection.JavaConverters._
import com.hp.hpl.jena.rdf.model.{Model, ResourceFactory, Resource => JResource}
import java.security.cert.X509Certificate


/**
 * Some initial ideas on mappers between rdf and scala classes
 * exploring ideas from
 * http://dl.dropbox.com/u/7810909/docs/reader-monad/chunk-html/index.html
 *
 * @author Henry Story
 */


/**
 * Resource readers are Monads
 * These function directly on types given by resources specified by URIs
 * But the map from resource to validation, makes things a bit heavy.
 */
case class ResourceReader[A](extract: Resource => Validation[scala.Throwable,A]) {
  def map[B](g: A => B) = ResourceReader(r => extract(r).map(g(_)))
  def flatMap[B](g: A => ResourceReader[B]): ResourceReader[B] = ResourceReader(r => extract(r).map(g(_)).flatMap(_.extract(r)))
}

trait Agent {
  def name: String
  def foaf(attr: String): Iterator[AnyRef] = Iterator.empty
  def depictions: Iterator[JResource] = Iterator.empty
}

object CertAgent {
  val CNregex = "cn=(.*?),".r
}

/**
 *Information about an agent gleaned from the certificate
 *(One could generalise this by having a function from certificates to graphs)
 **/
class CertAgent(dn : String) extends Agent {
  val name =  CertAgent.CNregex.findFirstMatchIn(dn).map(_.group(1)).getOrElse("(unnamed)")

  override def foaf(attr: String) = if (attr == "name") Iterator(name) else Iterator.empty

}

case class Person(name: String)



case class IdPerson(id: JResource) extends Agent {


  import Extractors.toProperty
  // a couple of useful methods for foaf relations. One could add a few others for other vocabs
  override def foaf(attr: String) = id.listProperties(toProperty(FOAF.NS+attr)).asScala.map(_.getObject)

  //very very simple implementation, not taking account of first/last name, languages etc...
  override def name = {
    foaf("name").mkString(" ")
  }
  
  override def depictions = foaf("depiction").collect{case n if n.isURIResource => n.asResource()}

}

object ANONYMOUS extends Agent {
  val pix = ResourceFactory.createResource("http://massivnews.com/wp-content/uploads/2011/07/Anonymous-000006.jpg")
  override val name = "_ANONYMOUS_"
  override def foaf(attr: String) = if (attr == "name") Iterator(name) else Iterator.empty
  override val depictions = List(pix).iterator
}


object Extractors {
  type Val[A] = Validation[scala.Throwable,A]
 
  def findPeople(m: Resource): Validation[scala.Throwable,Set[Person]] = {
     for (gr<-m.get) yield {
       for (st <- gr.listStatements(null,RDF.`type`,FOAF.Person).asScala;
        val subj = st.getSubject;
        st2 <- gr.listStatements(subj, FOAF.name,null).asScala
        ) yield {
         new Person(st2.getObject.asLiteral().toString)
        }
     }.toSet
  }

  def definedPeople(gr: Model, doc: URL): Iterator[IdPerson] = {
    for (st <- gr.listStatements(null, RDF.`type`, FOAF.Person).asScala;
         val subj = st.getSubject;
         //todo: come up with a better definition of "is defined in"
         if (subj.isURIResource && subj.toString.split("#")(0) == doc.toString.split("#")(0));
         st2 <- gr.listStatements(subj, FOAF.name, null).asScala
    ) yield {
      new IdPerson(subj)
    }
  }

  /**
   * Argh. Jena does not make a good difference between read only models, and RW ones
   * So one should verify the person exists before doing this if one does not want to create a RW model
   */
  def namedPerson(gr: Model, webid: URL): IdPerson = {
      IdPerson(gr.createResource(webid.toString))
  }

  def findDefinedPeople(m: Resource): Validation[scala.Throwable,Set[IdPerson]] = {
    for (gr<-m.get) yield {
      definedPeople(gr, m.name)
    }.toSet
  }
  
  def findIdPeople(m: Resource): Val[Set[IdPerson]] = {
    for (gr<-m.get) yield {
      for (st <- gr.listStatements(null,RDF.`type`,FOAF.Person).asScala;
           val subj = st.getSubject;
           if (subj.isURIResource)
      ) yield {
        val p = new IdPerson(subj)

        p
      }
    }.toSet
    
  } 
  
  implicit def toResource(str: String) = ResourceFactory.createResource(str)
  implicit def toProperty(str: String) = ResourceFactory.createProperty(str)

}

object Test {
  implicit def urlToResource(u: URL) = WebCache.resource(u)
  import System._

  val peopleRd = new ResourceReader[Set[Person]](Extractors.findPeople)
  val definedPeopleRd = new ResourceReader[Set[IdPerson]](Extractors.findDefinedPeople)
  val idPeopleRd = new ResourceReader[Set[IdPerson]](Extractors.findIdPeople)
  val definedPeopleFriends = definedPeopleRd.flatMap(people =>ResourceReader[Set[IdPerson]]{
    resource: Resource =>
       resource.get.map(gr=>
         for ( p <- people;
               st <- gr.listStatements(p.id, FOAF.knows, null).asScala ;
              val friend = st.getObject;
              if (friend.isURIResource)
         ) yield IdPerson(friend.asInstanceOf[JResource])
       )
  } )
  
  def main(args: Array[String]) {

    val url: String = "http://bblfish.net/people/henry/card"

    // extract the people who are defined in the graph (rarely more than one)
    for (people <- definedPeopleRd.extract(new URL(url));
         p <- people) {
      System.out.println("found "+p.name)
    }
    out.println

    out.println("friends of people defined using flatmaped reader")
    //use the flatMap to go from defined people to their friends
    //get these friends names
    for (people <- definedPeopleFriends.extract(new URL(url));
         p <- people) {
      System.out.println("found "+p.name)
    }
    out.println



    // extract all the people with ids.
    // and show all their properties
    // this produces a lot of data so its commented out
//    out.println("=== ID PEOPLE ===")
//    out.println
//    for (people <- idPeopleRd.extract(new URL(url));
//         p <- people) {
//      out.println
//      out.println("----------")
//      out.println("id "+p.webid)
//      out.println("with properties:")
//      val str = for ((prop,setovals) <- p.relations.iterator) yield {
//        setovals.map(n=>prop.getLocalName+" is "+n.toString).mkString("\n")
//      }
//      out.print(str.mkString("\r\n"))
//    }

  }
}