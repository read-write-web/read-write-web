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
import java.net.{URI, URL}
import collection._
import com.hp.hpl.jena.rdf.model.{RDFNode, Literal, ResourceFactory, ModelFactory, Resource => JResource, Model}
import collection.JavaConverters._


/**
 * Some initial ideas on mappers between rdf and scala classes
 * exploring ideas from
 * http://dl.dropbox.com/u/7810909/docs/reader-monad/chunk-html/index.html
 *
 * @author Henry Story
 */

case class GraphReader[A](extract: Resource => Validation[scala.Throwable,A]) {
  def map[B](g: A => B) = GraphReader(r => extract(r).map(g(_)))
  def flatMap[B](g: A => GraphReader[B]): GraphReader[B] = GraphReader(r => extract(r).map(g(_)).flatMap(_.extract(r)))
}

case class Person(name: String)

case class IdPerson(webid: JResource) {

  // this map is no longer necessary if we use the graph mapped resource
  val relations : mutable.MultiMap[JResource,RDFNode] = new mutable.HashMap[JResource, mutable.Set[RDFNode]] with  mutable.MultiMap[JResource,RDFNode]
  cacheProp

  import Extractors.toProperty
  // a couple of useful methods for foaf relations. One could add a few others for other vocabs
  def foafRel(p: String) = relations.get(toProperty(FOAF.NS+p))

  //very very simple implementation, not taking account of first/last name, languages etc...
  def name = {
    foafRel("name").mkString(" ")
  }

  /**
   * Notice how here the graph has been mapped into the object, via the webid JResource that still
   * has a pointer to the graph.
   */
  def cacheProp {
    for (s <- webid.listProperties().asScala
         if (!s.getObject.isAnon)
    ) {
      relations.addBinding(s.getPredicate, s.getObject)
    }
  }

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

  def findDefinedPeople(m: Resource): Validation[scala.Throwable,Set[IdPerson]] = {
    for (gr<-m.get) yield {
      for (st <- gr.listStatements(null,RDF.`type`,FOAF.Person).asScala;
           val subj = st.getSubject;
           if (subj.isURIResource && subj.toString.startsWith(m.name.toString));
           st2 <- gr.listStatements(subj, FOAF.name,null).asScala
      ) yield {
        new IdPerson(subj)
      }
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

  val peopleRd = new GraphReader[Set[Person]](Extractors.findPeople)
  val definedPeopleRd = new GraphReader[Set[IdPerson]](Extractors.findDefinedPeople)
  val idPeopleRd = new GraphReader[Set[IdPerson]](Extractors.findIdPeople)
  val definedPeopleFriends = definedPeopleRd.flatMap(people =>GraphReader[Set[IdPerson]]{
    resource: Resource =>
       resource.get.map(gr=>
         for ( p <- people;
               st <- gr.listStatements(p.webid, FOAF.knows, null).asScala ;
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