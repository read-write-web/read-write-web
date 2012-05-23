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
* embedded Web server (Jetty or Netty)
* tests for web api (specs)
* logger (slf4j)
* the Jena/ARQ libraries

Community
---------

For any improvement ideas and and bugfixes send e-mail to henry.story@bblfish.net  

This application is based on standards that the following groups are discussing:
 * the read-write-web community group at http://www.w3.org/community/rww/
 * the WebID community group at http://www.w3.org/community/webid/


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

    > run 

You are then given a choice of using either the Jetty or the Netty Server.  
( The Netty Server has better SSL/TLS and hence beter WebID support. )
You will then get a listing of all the options.

The following is known to work with https

To start an insecure server start
   
    > run --lang turtle  --http 8080 test_www /2012/ 

ou can now request resources using curl on the command line

   $ curl -i -H "Accept: application/rdf+xml" http://localhost:8080/2012/foaf.n3 

It is possible to PUT, RDF resources too and a couple of image formats, as 
well as to create directories etc... The test suites have some good examples of this. 

### to generate the eclipse configuration

    > eclipse same-targets

### to generate the IntelliJ configuration
  
    > gen-idea

### to package the application as a stand-alone jar (creates target/read-write-web.jar)

    > assembly

### run RWW using the standalone jar

    $ java -jar target/read-write-web.jar 

### to enable debug mode in sbt 

If you want to debug SBT using an IDE the start sbt with  

   $ bin/rwsbt.sh -d      

    
Access control with WebID 
-------------------------

### start server on https port with WebID ( http://webid.info/ )
    
    > run --lang turtle --keyStore src/test/resources/KEYSTORE.jks --clientTLS noCA --ksPass secret --https 8443 test_www /2012/

You can now request resources using curl over https using the command line. 
Note: If the server itself needs to make requests over TLS for WebID authentication this will start in insecure [ --clientTLS noCA ] mode so that the server will not refuse a connection if it reaches a server (like itself) which does not have a CA signed certificate .

The following file is readable by all and so you will be able to access it:

    $ curl -k -i -H "Accept: application/rdf+xml" https://localhost:8443/2012/foaf.n3 
    HTTP/1.1 200 OK
    ...

In the test_www directory there is a .meta.n3 file which contains the access rules as defined by the http://www.w3.org/wiki/WebAccessControl vocabulary. ( this is still a very minimal implementation of what is possible ) These say that private.n3 requires authentication. And indeed:
 
    $ curl -k -i -H "Accept: application/rdf+xml" https://localhost:8443/2012/private.n3
    curl: (56) SSL read: error:14094412:SSL routines:SSL3_READ_BYTES:sslv3 alert bad certificate, errno 0

Curl returns a SSL error, because the server asked curl for a client certificate which it did not offer and because curl forces the server to return the certificate ( which could be considered a bug IMHO ). A browser would return a 401 Unauthorized, with some extra data explaining the error.  What would be nice would be to see this:

    $ curl -k -i -H "Accept: application/rdf+xml" https://localhost:8443/2012/hello.n3
    HTTP/1.1 401 Unauthorized
    Server: Scala Netty Unfiltered Server
    Connection: Keep-Alive
    Content-Length: 0

In order to be able to access protected.n3 you need to use a WebID enabled certificate 
when connecting and add that WebID to the access controlfile .meta.n3
 
    $ curl -k -i -E src/test/resources/JohnDoe.pem https://localhost:8443/2012/private.n3
      Enter PEM pass phrase:
      HTTP/1.1 200 OK

[ the curl password is "secret" ]

### creating your own certificate

You can create your own certificate in your browser using a service such as https://my-profile.eu/
This will save your certificate in the keychain associated with the browser. 
You can then edit the .meta.n3 file to give yourself read/write/execute access using that certificate.

If you want to use that certificate using curl, follw these steps:
* save the certificate to your hardrive as a pkcs12 ( p12 ) file 
* convert it to pem including private key
    > openssl pkcs12  -in cert.p12 -out myCert.pem

Extra Userful Services provided  
-------------------------------

### verify your key 

If you have trouble with your certificate you can test if it is being verified correctly
with

   $ curl -k --verbose -i -E myCert.pem https://localhost:8443/test/WebId

### simple authentication service

RWW comes with a simple authentication service that you can use to help
others who don't have WebID to get going without needing to deploy TLS 
services. Go to https://localhost:8443/srv/idp in your browser .

This of course is not installed if you run as server on --http and it is
disabled for Jetty, as in Jetty I have not found how to do TLS renegotiation
so that the certificate does not need to be requested if the resource is not
protected. (On such servers one needs authentication to happen on a different 
port )

TODO
----

There is still a lot to do. Some things we are working on:

* improve asynchronous behavior using akka.io 
* make it easy to switch between Jena, Sesame and other frameworks using [https://github.com/w3c/banana-rdf/](banana-rdf
* improve the access control reasoning (which is very very basic for the moment)
* improve architecture to work more fluidly with non RDF resources, such as pictures or videos
* enrich the HTTP headers with the metadata for the access control files (so that one can follow one's nose)


