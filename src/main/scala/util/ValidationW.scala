package org.w3.readwriteweb.util

import scalaz._
import Scalaz._

trait ValidationW[E, S] {

  val validation: Validation[E, S]
 
  def failMap[EE](f: E => EE): Validation[EE, S] =
    validation.fail map f validation

}