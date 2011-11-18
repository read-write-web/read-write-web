package org.w3.webacl

import java.net.{URI, URL}
import scala.util.matching.Regex

trait Authorization[A <: Action] {
  
  val agentPolicies: Set[AcceptedAgentPolicy]
  val actions: Set[A]
  val accessTo: Set[ResourcePolicy]
  
  final def authorized(
      agent: Agent,
      action: A,
      accessedResource: URL): Boolean = {
    def agentIsConcernedBySomePolicy = agentPolicies exists { _ concerns agent }
    def knownAction = actions contains action
    def concernedWebResource = accessTo exists { _ concerns accessedResource }
    agentIsConcernedBySomePolicy && knownAction && concernedWebResource
  }
  
}

trait AcceptedAgentPolicy {
  def concerns(agent: Agent): Boolean
}

/** NORMATIVE Policy that concerns any agent
  * note: the agent is not challenged
  */
case object anybody extends AcceptedAgentPolicy {
  def concerns(agent: Agent): Boolean = true
}

/** NORMATIVE Policy that concerns a single agent, identified by its URI
  */
case class SingleAgent(uri: URI) extends AcceptedAgentPolicy {
  def concerns(agent: Agent): Boolean = agent.authenticatedAs == Some(uri)
}

// TODO policy for a group of agent
// TODO policy that aggregates other policies

trait Agent {
  /**
    * @return the identity of the agent if the authentication challenge is successful
    */
  def authenticatedAs: Option[URI]
}

case class UserPassword(id: URI, user: String, password: String) extends Agent {
  val authenticatedAs: Option[URI] = Some(id)
}

case class WebId(url: URL) extends Agent {
  val authenticatedAs: Option[URI] = Some(url.toURI)
}

case class BrowserId(email: String) extends Agent {
  val authenticatedAs: Option[URI] = Some(new URI(email))
}

trait Action

trait ResourcePolicy {
  def concerns(url: URL): Boolean
}

/**
  * NORMATIVE concerns resources identified by its authoritative representation
  */
case class AuthoritativeResource(authoritative: URL) extends ResourcePolicy {
  def concerns(url: URL): Boolean = authoritative == url
}

/**
  * NORMATIVE concerns resources whose (not necessarily direct) parent is parent
  */
case class ParentResource(parent: URL) extends ResourcePolicy {
  def concerns(url: URL): Boolean = url.toString startsWith parent.toString
}

/**
  * NORMATIVE concerns resources that match regex
  */
case class RegexBasedResource(regex: Regex) extends ResourcePolicy {
  def concerns(url: URL): Boolean = regex.findFirstIn(url.toString).isDefined
}

