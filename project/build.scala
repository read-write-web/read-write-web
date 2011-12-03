import sbt._
import Keys._

// some useful libraries
// they are pulled only if used
object Dependencies {
//  val specs = "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"
  val specs2 = "org.specs2" %% "specs2" % "1.6.1"
  val specs2_scalaz =  "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test"
  val dispatch_version = "0.8.6"
  val dispatch_http = "net.databinder" %% "dispatch-http" % dispatch_version 
//  val dispatch_nio = "net.databinder" %% "dispatch-nio" % dispatch_version 
  val unfiltered_version = "0.5.2"
  val unfiltered_filter = "net.databinder" %% "unfiltered-filter" % unfiltered_version 
  val unfiltered_jetty = "net.databinder" %% "unfiltered-jetty" % unfiltered_version 
  val unfiltered_netty = "net.databinder" %% "unfiltered-netty" % unfiltered_version 
  val scalate = "net.databinder" %% "unfiltered-scalate" % "0.5.1"
  // val unfiltered_spec = "net.databinder" %% "unfiltered-spec" % "0.4.1" % "test"
  val ivyUnfilteredSpec =
    <dependencies>
      <dependency org="net.databinder" name="unfiltered-spec_2.9.1" rev={unfiltered_version}>
        <exclude org="net.databinder" module="dispatch-mime_2.9.0-1"/>
      </dependency>
    </dependencies>
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.4"
  val antiXML = "com.codecommit" %% "anti-xml" % "0.4-SNAPSHOT" % "test"
  val jena = "com.hp.hpl.jena" % "jena" % "2.6.4"
  val rdfa = "net.rootdev" % "java-rdfa" % "0.4.2-RC2"
  val htmlparser = "nu.validator.htmlparser" % "htmlparser" % "1.2.1"
  val arq = "com.hp.hpl.jena" % "arq" % "2.8.8"
  val grizzled = "org.clapper" %% "grizzled-scala" % "1.0.8" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.3"
  val argot =  "org.clapper" %% "argot" % "0.3.5"
  val guava =  "com.google.guava" % "guava" % "10.0.1"
//  val restlet = "org.restlet.dev" % "org.restlet" % "2.1-SNAPSHOT"
//  val restlet_ssl = "org.restlet.dev" % "org.restlet.ext.ssl" % "2.1-SNAPSHOT"
  val jsslutils = "org.jsslutils" % "jsslutils" % "1.0.5"
}

// some usefull repositories
object Resolvers {
  val novus = "repo.novus snaps" at "http://repo.novus.com/snapshots/"
  val mavenLocal = "Local Maven Repository" at "file://" + (Path.userHome / ".m2" / "repository").absolutePath
}

// general build settings
object BuildSettings {

  val buildOrganization = "org.w3"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.9.1"

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
  import ProguardPlugin._
  import sbtassembly.Plugin._
  import sbtassembly.Plugin.AssemblyKeys._
  

  def keepUnder(pakage:String):String = "-keep class %s.**" format pakage
  
  val proguardSettings:Seq[Setting[_]] =
    ProguardPlugin.proguardSettings ++ Seq[Setting[_]](
      minJarPath := new File("readwriteweb.jar"),
      proguardOptions += keepMain("org.w3.readwriteweb.ReadWriteWebMain"),
      proguardOptions += keepUnder("org.w3.readwriteweb"),
      proguardOptions += keepUnder("unfiltered"),
      proguardOptions += keepUnder("org.apache.log4j"),
      proguardOptions += keepUnder("com.hp.hpl.jena"),
      proguardOptions += "-keep class com.hp.hpl.jena.rdf.model.impl.ModelCom"
    )

  val projectSettings =
    Seq(
      resolvers += mavenLocal,
      resolvers += ScalaToolsReleases,
      resolvers += ScalaToolsSnapshots,
      libraryDependencies += specs2,
      libraryDependencies += specs2_scalaz,
//      libraryDependencies += unfiltered_spec,
      ivyXML := ivyUnfilteredSpec,
      libraryDependencies += dispatch_http,
      libraryDependencies += dispatch_nio,
      libraryDependencies += unfiltered_filter,
      libraryDependencies += unfiltered_jetty,
      libraryDependencies += unfiltered_netty,
      libraryDependencies += slf4jSimple,
      libraryDependencies += jena,
      libraryDependencies += arq,
      libraryDependencies += antiXML,
      libraryDependencies += grizzled,
      libraryDependencies += scalaz,
      libraryDependencies += jsslutils,
      libraryDependencies += argot,
      libraryDependencies += guava,
      libraryDependencies += scalate,
      libraryDependencies += rdfa,
      libraryDependencies += htmlparser,

      jarName in assembly := "read-write-web.jar"
    )



  lazy val project = Project(
    id = "read-write-web",
    base = file("."),
    settings = buildSettings ++ assemblySettings ++ proguardSettings ++ projectSettings
  )
  


}

