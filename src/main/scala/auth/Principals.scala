/*
 * Copyright (c) 2011 Henry Story (bblfish.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms are permitted
 * provided that the above copyright notice and this paragraph are
 * duplicated in all such forms and that any documentation,
 * advertising materials, and other materials related to such
 * distribution and use acknowledge that the software was developed
 * by Henry Story.  The name of bblfish.net may not be used to endorse 
 * or promote products derived
 * from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.w3.readwriteweb.auth

import java.security.Principal

/**
 * @author hjs
 * @created: 13/10/2011
 */

/**
 * @author Henry Story from http://bblfish.net/
 * @created: 09/10/2011
 */

case class WebIdPrincipal(webid: String) extends Principal {
  def getName = webid
  override def equals(that: Any) = that match {
    case other: WebIdPrincipal => other.webid == webid
    case _ => false
  }
}

case class Anonymous() extends Principal {
  def getName = "anonymous"
  override def equals(that: Any) =  that match {
      case other: WebIdPrincipal => other eq this 
      case _ => false
    } //anonymous principals are equal only when they are identical. is this wise?
      //well we don't know when two anonymous people are the same or different.
}