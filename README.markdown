The ReadWriteWeb app
--------------------

It depends on:

* Java 6
* that's all :-)

It comes with

* sbt project
* generic sbt launcher
* jar packager (assembly)
* eclipse plugin for sbt
* Web framework (Unfiltered)
* embedded Web server (Jetty)
* tests for web api (specs)
* logger (slf4j)
* the Jena/ARQ libraries

How to start geeking
--------------------

BE PATIENT: the first time, some operations take some time because it downloads
            all the dependencies...

* to launch sbt

$ ./sbt

* to auto-compile the source

> ~ compile

* to launch tests under sbt (will cache all the dependencies the first time, can take a while)

> test

* to run the Web App

> run

or

> run 8080

* to generate the eclipse configuration

> eclipse same-targets

* to package the application as a stand-alone jar (creates target/read-write-web.jar)

> assembly

Using the stand-alone jar
-------------------------

java -jar target/read-write-web.jar 8080 ~/WWW/2011/09 /2011/09
