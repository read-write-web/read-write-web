The ReadWriteWeb app
--------------------

To get this:

    hg clone https://dvcs.w3.org/hg/read-write-web
    cd read-write-web
    less README.markdown
    ./sbt
    
* see [http://mercurial.selenic.com/](http://mercurial.selenic.com/) for hg
* see [https://github.com/harrah/xsbt/wiki](https://github.com/harrah/xsbt/wiki) for sbt

This project depends on:

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

### to launch sbt

    $ ./sbt

### to auto-compile the source

    > compile

### to launch tests under sbt (will cache all the dependencies the first time, can take a while)

    > test

### to run the Web App

Get the full command line options 

    > run --help

You are then given a choise of using either the Jetty or the Netty Server.  ( The Netty Server has better WebID support.)
You will then get a listing of all the options.

The following is known to work with https

   > run --lang turtle --keyStore src/test/resources/KEYSTORE.jks --ksPass secret --https 8443 test_www /2012/

Because of the .meta.n3 in the test_www directory you will not be able to GET the contents there


The ReadWriteWeb app
--------------------

To get this:

    hg clone https://dvcs.w3.org/hg/read-write-web
    cd read-write-web
    less README.markdown
    ./sbt
    
* see [http://mercurial.selenic.com/](http://mercurial.selenic.com/) for hg
* see [https://github.com/harrah/xsbt/wiki](https://github.com/harrah/xsbt/wiki) for sbt

This project depends on:

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

### to launch sbt

    $ ./sbt

### to auto-compile the source

    >  compile

### to launch tests under sbt (will cache all the dependencies the first time, can take a while)

    > test

### to run the Web App and get the full command line options

    > run

    Choose either the Netty or the Jetty servers to continue.
    You will get a full list of options

    To start an insecure server start
   
    > run --lang turtle  --http 8080 test_www /2012/ 

   You can now request resources using curl on the command line

   $ curl -i -H "Accept: application/rdf+xml" http://localhost:8080/2012/foaf.n3 

   It is possible to PUT, RDF resources too and a couple of image formats, as 
   well as to create directories etc...

### to generate the eclipse configuration

    > eclipse same-targets

### to generate the IntelliJ configuration
  
    > gen-idea

### to package the application as a stand-alone jar (creates target/read-write-web.jar)

    > assembly

Using the stand-alone jar
-------------------------

    $ java -jar target/read-write-web.jar 

    
HTTPS with WebID 
----------------

### to run on https with WebID ( http://webid.info/ )
    
   > run --lang turtle --keyStore src/test/resources/KEYSTORE.jks --ksPass secret --https 8443 test_www /2012/

   You can now request resources using curl over https using the command line

   $ curl -k -i -H "Accept: application/rdf+xml" https://localhost:8443/2012/foaf.n3

   In the test_www directory there is a meta.n3 file. If you move it to .meta.n3

   $ cd test_www
   $ mv meta.n3 .meta.n3

   Then the directory will be access controlled. You will need a functioning WebID to authenticate.

### to enable debug mode

    start sbt with the 

    $ bin/rwsbt.sh -d      

### to generate the eclipse configuration

    > eclipse same-targets

### to generate the IntelliJ configuration
  
    > gen-idea

### to package the application as a stand-alone jar (creates target/read-write-web.jar)

    > assembly

