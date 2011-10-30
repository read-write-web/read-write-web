#!/bin/bash 

KS=src/test/resources/KEYSTORE.jks
while [ $# -gt 0 ] 
do 
 case $1 in 
  -d) PROPS="$PROPS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
   ;; 
  -n) PROPS="$PROPS -Dnetty.ssl.keyStoreType=JKS -Dnetty.ssl.keyStore=$KS -Dnetty.ssl.keyStorePassword=secret" 
   ;;
  -j) PROPS="$PROPS -Djetty.ssl.keyStoreType=JKS -Djetty.ssl.keyStore=$KS -Djetty.ssl.keyStorePassword=secret"
   ;;
  -sslUnsafe) PROPS="$PROPS -Dsun.security.ssl.allowUnsafeRenegotiation=true"
   ;; # see: http://download.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#workarounds
  -sslLegacy) PROPS="$PROPS -Dsun.security.ssl.allowLegacyHelloMessages=true"
   ;;
  -sslDebug) PROPS="$PROPS -Djavax.net.debug=all"
   ;; # see http://download.oracle.com/javase/1,5,0/docs/guide/security/jsse/ReadDebug.html
  *) echo the arguments to use are -d
   ;;
  esac
  shift 1
 done


export SBT_PROPS=$PROPS
xsbt 
