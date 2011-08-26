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

* to package the application as a standalone jar (ends up under target/ directory)

> assembly

* to generate the eclipse configuration

> eclipse same-targets

Have fun!
