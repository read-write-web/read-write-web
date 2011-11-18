package org.w3.webacl

sealed trait RWWAction extends Action
case object read extends RWWAction
case object write extends RWWAction
case object append extends RWWAction
case object control extends RWWAction

trait RWWAuthorization extends Authorization[RWWAction]