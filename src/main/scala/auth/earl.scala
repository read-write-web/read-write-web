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

package org.w3.readwriteweb.auth

import com.hp.hpl.jena.vocabulary.DCTerms
import java.security.interfaces.RSAPublicKey
import org.w3.readwriteweb.util.trySome
import java.lang.ref.WeakReference
import com.hp.hpl.jena.rdf.model.{Model, Property, ModelFactory}
import scalaz.{Failure, Success}

/**
 * Classes for the tests in WebID Authentication.
 *
 * The idea is to try to use the earl test cases we are defining at the WebID XG as
 * a way of collecting tests done to prove the X509Claim and the WebIDClaim.
 *
 * These tests are now very close to the way the code is working. So much so that one can get
 * feeling that perhaps by adding the URIs of these tests directly to the error codes one
 * could merge a few classes. So perhaps with a bit of tuning a few classes here could just
 * disappear.
 *
 */


object Tests {
  // This is where the earl tests are documented and named
  val ns = "http://www.w3.org/2005/Incubator/webid/earl/RelyingParty"
  val skos = "http://www.w3.org/2004/02/skos/core#"

  private var m : WeakReference[Model] = null
  
  //todo: this model should be a weak pointer
   def model= {
     if (m==null || m.get() == null) m = new WeakReference( ModelFactory.createDefaultModel().read(
       this.getClass.getResourceAsStream("/ontologies/RelyingParty.n3"),
       ns,  "TURTLE" ) )
     m.get;
   }


}


trait Test {
  val title: String
  val description: String
  val note: String
}

/**
 * Test with extra information taken from the ontologies
 */
abstract class OntTest(val name: String) extends Test {
  import Tests._
  implicit def boolToResult(bool: Boolean): Outcome = if (bool) passed else failed

  private def value(p: Property) = trySome(resource.getProperty(p).getLiteral.getLexicalForm) getOrElse "-missing-"
  
  val title: String = value(DCTerms.title)
  val description: String = value(DCTerms.description)
  val note: String = value(model.createProperty(skos,"note"))

  private def resource = model.getResource(ns+"#"+name)
}

abstract class TestObj[T](name: String) extends OntTest(name) {
  def apply(subj: T): Result
  def depends(subj: T): List[Assert] =Nil
}

/**
 * tests that we apply only to Exceptions
 */
class TestErr(name: String) extends OntTest(name)

//todo: (bblfish:) I get the feeling that one could put the logic into the tests directly.
//      would that make things easier or better?

//
//The types of tests that we do here
//

//for X509Claim
//object certProvided[X509Certificate] extends OntTest("certificateProvided")   {
//  def apply(cert: Option[X509Certificate]): Assertion = cert match {
//    case Some(x509) => new Assertion(this,passed,"got certificate") //the subject is the session
//    case None => new Assertion(this,failed,"missing certificate")
//  }
//}

object certOk extends TestObj[X509Claim]("certificateOk") {
   val parts = List[TestObj[X509Claim]](certDateOk, certProvidedSan,certPubKeyRecognized)

   def apply(x509: X509Claim): Result = {
      val deps = depends(x509)
      val res = deps.forall(_.result.outcome == passed)
      new Result(if (res) "X509 Certificate Good" else "X509 Certificate had problems",
        res,  deps)
   }
   override def depends(x509: X509Claim): List[Assert] = parts.map(test=>new Assertion[X509Claim](test,x509))
}

object certProvidedSan extends TestObj[X509Claim]("certificateProvidedSAN") {
  def apply(x509: X509Claim) = new Result(" There are "+x509.claims.size+" SANs in the certificate",
    x509.claims.size >0)

}

object certDateOk extends TestObj[X509Claim]("certificateDateOk") {
  def apply(x509: X509Claim) = 
    new Result("the x509certificate " + (
      if (x509.tooEarly) "is not yet valid "
      else if (x509.tooLate) " passed its validity date "
      else " is valid"),
      x509.isCurrent)

}

object certPubKeyRecognized extends TestObj[X509Claim]("certificatePubkeyRecognised") {
  def apply(claim: X509Claim) = {
    val pk = claim.cert.getPublicKey;
    new Result("We only support RSA Keys at present. ",pk.isInstanceOf[RSAPublicKey] )
  }
}

//for WebIDClaims
object webidClaimTst extends TestObj[WebIDClaim]("webidClaim") {
  def apply(webIdclaim: WebIDClaim) : Result =
    webIdclaim.verify match {
      case Success(webId) => new Result("WebId successfully verified",passed)
      case Failure(webIdClaimErr: WebIDVerificationFailure) => 
        new Result("keys in certificate don't match key in profile for "+webIdClaimErr.subject,failed)
      case Failure(e) => new Result("WebID verification failed",failed,cause=List(new AssertionErr(e)))
    }
  
  override def depends(webIdclaim: WebIDClaim): List[Assert] =
    webIdclaim.verify match {
      case Failure(e) if  !e.isInstanceOf[WebIDVerificationFailure] => new AssertionErr(e)::Nil
      case _ => Nil
    }
}



object profileGetTst extends TestErr("profileGet")
object profileParseTst extends TestErr("profileWellFormed")
object sanOk extends TestErr("sanOK")

//object profileOkTst extends OntTest("profileOk") {
//  def test(err: ProfileError): Assertion = new Assertion(this,err)::err match {
//    case getError : ProfileGetError => profileGetTst.test(getError)
//    case ProfileParseError(parseError: Fail) => new Assertion(profileParseTst,parseError)
//  }
//}
//object profileWellFormedKeyTst extends OntTest("profileWellFormedPubkey")

object Assertion {
  def name(fail: WebIDClaimFailure): Test = {
    fail match {
      case e: UnsupportedProtocol => sanOk
      case e: URISyntaxError => sanOk
      case e: ProfileGetError =>  profileGetTst
      case e: ProfileParseError => profileParseTst
      case e: UnsupportedKeyType => certPubKeyRecognized
    }
  }
}

trait Assert {
  val subject: AnyRef
  val test: Test
  val result: Result
}

class Assertion[T<:AnyRef]( val test: TestObj[T], val subject: T ) extends Assert {
  override val result: Result =  test(subject)
  val depends: List[Assert] = test.depends(subject)
}

class AssertionErr(val fail: WebIDClaimFailure) extends Assert {
  import Assertion.name
  val subject = fail.subject
  val test = name(fail)
  val result: Result = new Result(fail.msg,failed)
}

class Result (val description: String,
              val outcome: Outcome,
              val cause: List[Assert] = Nil)



sealed class Outcome(val name: String)  {
  val earl = "http://www.w3.org/ns/earl#"
  val id = earl+name
}

object untested extends Outcome("untested")
object passed extends Outcome("passed")
object failed extends Outcome("failed")


