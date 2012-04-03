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

    > ~ compile

### to launch tests under sbt (will cache all the dependencies the first time, can take a while)

    > test

### to run the Web App

    > run

or

    > run 8080

### to generate the eclipse configuration

    > eclipse same-targets

### to package the application as a stand-alone jar (creates target/read-write-web.jar)

    > assembly

Using the stand-alone jar
-------------------------

    java -jar target/read-write-web.jar 8080 ~/WWW/2011/09 /2011/09  [options]

Options:

 *   --relax   All documents exist as empty RDF files (like a wiki).
 *   --strict  Documents must be created using PUT else they return 404

To run with WebID see next section.
    
    
HTTPS with WebID 
----------------

### to run on https with WebID
    
    Wether you run the binary from the command line as described below or run it directly inside sbt you need to set the following parameters to java
 * -Djetty.ssl.keyStoreType=JKS                              - the keystore type (usually JKS)
 * -Djetty.ssl.keyStore=src/test/resources/KEYSTORE.jks      - the path to the keystore  
 * -Djetty.ssl.keyStorePassword=secret                       - the secret password for the keystore
 * -Dsun.security.ssl.allowUnsafeRenegotiation=true          - to allow unsafe TLS renegotiation
 * -Dsun.security.ssl.allowLegacyHelloMessages=true          - to allow legacy TLS hello Messages

The sun.security options are described in more detail http://download.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#workarounds
So to run the compiled jar you can use

   > java -Djetty.ssl.keyStoreType=JKS -Djetty.ssl.keyStore=/Users/hjs/tmp/cert/KEYSTORE.jks -Djetty.ssl.keyStorePassword=secret -jar target/read-write-web.jar --https 8443 www_test /2011/09

or to run sbt use the shorthand options in the bin/rwsbt.sh shell script eg

  > bin/rwsbt.sh -n -sslUnsafe -sslLegacy

(exercise: improve the script so that all options can be set with it)
### to enable debug add the following parameters after 'java'

     -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

