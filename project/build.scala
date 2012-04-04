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
  val dispatch_nio = "net.databinder" %% "dispatch-nio" % dispatch_version 
  val unfiltered_version = "0.6.1"
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
  val antiXML = "com.codecommit" %% "anti-xml" % "0.3" % "test"
  val jena = "org.apache.jena" % "jena-core" % "2.7.0-incubating"
  val arq = "org.apache.jena" % "jena-arq" % "2.9.0-incubating"
  val rdfa = "net.rootdev" % "java-rdfa" % "0.4.2-RC2"
  val htmlparser = "nu.validator.htmlparser" % "htmlparser" % "1.2.1"
  val grizzled = "org.clapper" %% "grizzled-scala" % "1.0.8" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"
  val argot =  "org.clapper" %% "argot" % "0.3.5"
  val guava =  "com.google.guava" % "guava" % "11.0"
//  val restlet = "org.restlet.dev" % "org.restlet" % "2.1-SNAPSHOT"
//  val restlet_ssl = "org.restlet.dev" % "org.restlet.ext.ssl" % "2.1-SNAPSHOT"
  val jsslutils = "org.jsslutils" % "jsslutils" % "1.0.5"
  val slf4s = "com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7"
//  val akka_actor = "com.typesafe.akka" % "akka-actor" % "2.0-M3"
  val async_http_client = "com.ning" % "async-http-client" % "1.7.0"
  val aalto_xml = "com.fasterxml" % "aalto-xml" % "0.9.7"
//  val akka_remote = "com.typesafe.akka" % "akka-remote" % "2.0-M3"
//  val finagle_http = "com.twitter" %% "finagle-http" % "1.9.12"
}

// some usefull repositories
object Resolvers {
  val novus = "repo.novus snaps" at "http://repo.novus.com/snapshots/"
  val mavenLocal = "Local Maven Repository" at "file://" + (Path.userHome / ".m2" / "repository").absolutePath
  val typesafeSnap = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
  val sonatype = "Sonatype Release" at "http://oss.sonatype.org/content/repositories/releases"
//  val twitter = "Twitter Repository" at "http://maven.twttr.com/"
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
      proguardOptions += keepMain("org.w3.readwriteweb.netty.ReadWriteWebNetty"),
      proguardOptions += keepUnder("org.w3.readwriteweb"),
      proguardOptions += keepUnder("org.apache.xerces"),
      proguardOptions += keepUnder("unfiltered"),
      proguardOptions += keepUnder("org.apache.log4j"),
      proguardOptions += keepUnder("com.hp.hpl.jena"),
      proguardOptions += "-keep class com.hp.hpl.jena.rdf.model.impl.ModelCom",
      makeInJarFilter <<= (makeInJarFilter) {
        (makeInJarFilter) => {
          (file) => file match {
            case "slf4j-simple-1.6.4.jar" => makeInJarFilter(file) + ",!org/slf4j/**"
            case _ => makeInJarFilter(file)
          }
        }
      }
    )



  val projectSettings =
    Seq(
      resolvers ++= Seq( mavenLocal, sonatype, typesafeSnap, ScalaToolsReleases, ScalaToolsSnapshots ) ,
      libraryDependencies += specs2,
      libraryDependencies += specs2_scalaz,
//      libraryDependencies += unfiltered_spec,
      ivyXML := ivyUnfilteredSpec,
      libraryDependencies ++= Seq(dispatch_http, dispatch_nio, unfiltered_filter,
        unfiltered_jetty, unfiltered_netty,
        jena, arq, rdfa,
        grizzled,
        scalaz,
        jsslutils,
        argot,
        guava,
        scalate,
        htmlparser,
        slf4s,
        async_http_client,
        aalto_xml, antiXML) ,

      jarName in assembly := "read-write-web.jar",
      mainClass in assembly := Some("org.w3.readwriteweb.netty.ReadWriteWebNetty")
    )



  lazy val project = Project(
    id = "read-write-web",
    base = file("."),
    settings = buildSettings ++ assemblySettings ++ proguardSettings ++ projectSettings
  )
  


}

