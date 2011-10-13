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

import org.w3.readwriteweb.WebCache
import org.apache.shiro.authc.{AuthenticationInfo, AuthenticationToken}
import org.apache.shiro.subject.{SimplePrincipalCollection, PrincipalCollection}
import collection.JavaConversions
import org.apache.shiro.realm.{AuthenticatingRealm, AuthorizingRealm}
import org.apache.shiro.authz.AuthorizationInfo

/**
 * @author hjs
 * @created: 12/10/2011
 */

class WebIdRealm(cache: WebCache) extends AuthorizingRealm {

  def doGetAuthenticationInfo(token: AuthenticationToken): AuthenticationInfo = {
      val x509claim = token.asInstanceOf[X509Claim]
      val verified = for (
       claim <- x509claim.webidclaims;
       if (claim.verified)
     ) yield claim.principal


    return new AuthenticationInfo {
      def getPrincipals =  new SimplePrincipalCollection(JavaConversions.asJavaCollection(verified),"webid")

      def getCredentials = x509claim
    }
  }

  // not really sure what to do here
  //
  def doGetAuthorizationInfo(principals: PrincipalCollection) = new AuthorizationInfo {

    def getRoles = null

    def getStringPermissions = null

    def getObjectPermissions = null
  }


}

