import sbt._
import Keys._

// some usefull libraries
// they are pulled only if used
object Dependencies {
  val specs = "org.scala-tools.testing" % "specs_2.9.0-1" % "1.6.8" % "test"
  val scalatest = "org.scalatest" % "scalatest_2.9.0" % "1.6.1" % "test"
  val salat = "com.novus" %% "salat-core" % "0.0.8-SNAPSHOT"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.8.4"
  val unfiltered_filter = "net.databinder" %% "unfiltered-filter" % "0.4.1"
  val unfiltered_jetty = "net.databinder" %% "unfiltered-jetty" % "0.4.1"
  val unfiltered_scalate = "net.databinder" %% "unfiltered-scalate" % "0.4.1"
  val unfiltered_json = "net.databinder" %% "unfiltered-json" % "0.4.1"
  val unfiltered_spec = "net.databinder" %% "unfiltered-spec" % "0.4.1" % "test"
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.1"
  val antiXML = "com.codecommit" %% "anti-xml" % "0.3-SNAPSHOT"
  val jena = "com.hp.hpl.jena" % "jena" % "2.6.4"
  val arq = "com.hp.hpl.jena" % "arq" % "2.8.8"
//  val jenaIri = "com.hp.hpl.jena" % "iri" % "0.8" from "http://openjena.org/repo/com/hp/hpl/jena/iri/0.8/iri-0.8.jar"


}

// some usefull repositories
object Resolvers {
  val novus = "repo.novus snaps" at "http://repo.novus.com/snapshots/"
}

// general build settings
object BuildSettings {

  val buildOrganization = "org.w3"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.9.0-1"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion,
    parallelExecution in Test := false,
    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )

}

object YourProjectBuild extends Build {

  import Dependencies._
  import Resolvers._
  import BuildSettings._

  val yourProjectSettings =
    Seq(
      resolvers += ScalaToolsReleases,
      resolvers += ScalaToolsSnapshots,
      libraryDependencies += specs,
      libraryDependencies += unfiltered_spec,
      libraryDependencies += dispatch,
      libraryDependencies += unfiltered_filter,
      libraryDependencies += unfiltered_jetty,
      libraryDependencies += unfiltered_scalate,
      libraryDependencies += slf4jSimple,
      libraryDependencies += jena,
      libraryDependencies += arq
    )

  lazy val yourProject = Project(
    id = "your-project",
    base = file("."),
    settings = buildSettings ++ yourProjectSettings ++ sbtassembly.Plugin.assemblySettings
  )
  


}

