#!/bin/sh

#run the proguard compiled version of readwriteweb.

java -Dnetty.ssl.keyStoreType=JKS -Dsun.security.ssl.allowUnsafeRenegotiation=true -Dsun.security.ssl.allowLegacyHelloMessages=true -Dnetty.ssl.keyStore=`pwd`/src/test/resources/KEYSTORE.jks -Dnetty.ssl.keyStorePassword=secret  -Xms64M -Xmx700M -XX:+CMSClassUnloadingEnabled -classpath  readwriteweb.jar org.w3.readwriteweb.netty.ReadWriteWebNetty --https 8443 test_www /2011/09
