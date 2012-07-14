package test;

/*
 * Copyright (c) 2012 Henry Story (bblfish.net)
 * under the MIT licence defined at
 *    http://www.opensource.org/licenses/mit-license.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;


/**
 * This class was written for a bug report on the bugs.sun.com web site.
 *
 * The bug report contained the following:

 title: client only sends x509 certificate if requested through a NEED-client-Auth req
 =====================================================================================

 I noticed that a Java client connecting with HTTP to a TLS resource using the https:// protocol, only sends the certificate to the server IF the server has set the sslEngine.setNeedClientAuth(true). It does not send it if the sslEngine has set sslEngine.setWantClientAuth(true).

 I tested this on the OSX official Java VM and on Solaris. My feeling is that this is true across JDKs.

 ==========
 In order to create fully secure web site one needs to put all content behind
 https. If one does that one will then want to use the built in client
 authentication supported by the TLS protocol underlying https.

 [ As an important aside, note that client certificate authentication becomes even more useful in a WebID world ( http://webid.info/ ) . The WebID protocol ( http://webid.info/spec/ ) makes it possible for one client certificate to authenticate a user on any WebID enabled site he chooses to be identified on.  It is therefore going to be very likely that users will have a number of certificates in their browser to choose from depending on what persona they want to be known as at a particular web site at a particular time.  ]

 In order to respect the user's privacy and in order to give a fluid user experience it is very important that a secure web site _NOT_  ask the user for a client certificate when he first arrives on the web site. Humans don't read server certificates, and they usually want to make up their mind of whether or not to identify themselves AFTER they have had some experience of what the web site offers. Cryptographic Identification of the user on a TLS web site should therefore be done only when needed: when the user clicks the login button, or when he asks for a protected resource. (Lighter weight cookie identification is sufficient up to then) And even then, for a smooth UI experience the user should not be abruptly broken off in his request if he fails to provide a certificate. He should instead be redirected to a page where he can either create an account, or log in with OpenId, Facebook Connect, OAuth or all the other numerous existing identity standards...

 All of the above *is* possible due to TLS renegotiation. In TLS renegotiation
 the site can decide when the user needs to be identified and ask him only at
 that point to divulge his certificate.

 Now most web browsers ( other than Opera ) follow the above guidelines by sending servers a certificate if the server requests the certificate when the sslEngine has been parameterized  with setWantClientAuth(true) . This makes sense.

 Since one would like to use Java to build browsers too - as was initially the case with HotJava - it should be possible to set up java clients in the same way. Even when one creates crawlers it could be to send an error message to the client at the HTTP layer if his TLS connection fails to authenticate.

 What do you expect?
 ===================
 A Java Client connecting to an https server set up with setWantClientAuth(true) should send the certificate if requested by the server on renegotiation.


 [1] Simulate a client the server believes requires NEED mode
 ------------------------------------------------------------

 $ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId Java/1.6.0 ../resources/JohnDoe.p12

 This SHOULD send the certificate - and it does.

 [2] Simulate a client the server thinks is ok with WANT Mode
 ------------------------------------------------------------

 $ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId  Mozilla ../resources/JohnDoe.p12

 This should send the certificste too - it DOES NOT.

 [3] simulate a client the server knows requires NEED mode, but don't send a certificate (user wishes to be anonymous)
 ---------------------------------------------------------------------------------------------------------------------

 $ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId

 Here we connect to a resource as a JAva agent but do not send a certificate - we wish to remain anonymous. The Server should send us an HTTP error code and some helpful text if possible.

 What occurs?
 ============

 The Java Client only sends the certificate if the server is set up with setNeedClientAuth(true).

 In the source code pointed to below there are 3 test cases numbered [1]-[3]. They make calls on services that either

 * NEED authentication
 * Want authentication

 The server behavior for NEED or WANT can be triggered by changing the User-Agent field. This is because the server is keeping track of which user agents correctly deal with Want. This can be seen in the needAuth method of the X509Cert scala class:

 https://dvcs.w3.org/hg/read-write-web/file/023209cfc1e4/src/main/scala/auth/X509Cert.scala#l241

 Using the code below we can run the following tests:

 [1] Simulate a client the server knows requires NEED mode
 --------------------------------------------------------

 $ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId Java/1.6.0 ../resources/JohnDoe.p12

 the /test/WebID service requires authentication ( as it wants to test it ), and the server moves to NEED mode in order to be able to work with java clients. The java client sends the certificate and all is good.

 [2] Simulate a client the server thinks is ok with WANT Mode
 ------------------------------------------------------------

 $ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId  Mozilla ../resources/JohnDoe.p12

 Here the user agent is changed to one the server believes will be ok in WANT mode. The server reports back that it received no certificate. This is the problem: it should.

 [3] simulate a client the server knows requires NEED mode, but don't send a certificate (user wishes to be anonymous)
 ---------------------------------------------------------------------------------------------------------------------

 $ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId
 Exception in thread "main" javax.net.ssl.SSLHandshakeException: Received fatal alert: bad_certificate
 at sun.security.ssl.Alerts.getSSLException(Alerts.java:192)
 at sun.security.ssl.Alerts.getSSLException(Alerts.java:154)
 at sun.security.ssl.SSLSocketImpl.recvAlert(SSLSocketImpl.java:1943)
 at sun.security.ssl.SSLSocketImpl.readRecord(SSLSocketImpl.java:1059)
 at sun.security.ssl.SSLSocketImpl.readDataRecord(SSLSocketImpl.java:850)
 at sun.security.ssl.AppInputStream.read(AppInputStream.java:102)
 at java.io.BufferedInputStream.fill(BufferedInputStream.java:235)
 at java.io.BufferedInputStream.read1(BufferedInputStream.java:275)
 at java.io.BufferedInputStream.read(BufferedInputStream.java:334)
 at sun.net.www.http.HttpClient.parseHTTPHeader(HttpClient.java:633)
 at sun.net.www.http.HttpClient.parseHTTP(HttpClient.java:579)
 at sun.net.www.http.HttpClient.parseHTTP(HttpClient.java:604)
 at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1322)
 at java.net.HttpURLConnection.getResponseCode(HttpURLConnection.java:468)
 at sun.net.www.protocol.https.HttpsURLConnectionImpl.getResponseCode(HttpsURLConnectionImpl.java:338)
 at test.JavaTLSBugTest.connect(JavaTLSBugTest.java:75)
 at test.JavaTLSBugTest.main(JavaTLSBugTest.java:64)

 Here we get a bad_certificate error message, because indeed the server received no certificate and NEED required a certificate.

 Code
 ====

 Start the Read-Write-Web Server whose code is here
 https://dvcs.w3.org/hg/read-write-web/
 By doing the following

 $ hg clone https://dvcs.w3.org/hg/read-write-web
 $ cd read-write-web
 $ ./sbt
 > run-main org.w3.readwriteweb.netty.ReadWriteWebNetty --lang turtle --keyStore src/test/resources/KEYSTORE.jks --clientTLS noCA --ksPass secret --https 8443 test_www /2012/

 This starts the server on port 8443.

 Next we run the client, which is what we wish to test.  We have a test client which is available here: https://dvcs.w3.org/hg/read-write-web/file/tip/src/test/java/test/JavaTLSBugTest.java

 Compile it

 $ cd srt/test/java
 $ javac test/JavaTLSBugTest.java

 [1]$ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId Java/1.6.0 ../resources/JohnDoe.p12
 [2]$ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId  Mozilla ../resources/JohnDoe.p12
 [3]$ java -cp .  test.JavaTLSBugTest https://localhost:8443/test/WebId

 */
public class JavaTLSBugTest {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("missing arguments");
            usage();
        }
        URL url = new URL(args[0]);
        String ua = (args.length >= 2) ? args[1] : "Java/1.7.0";
        File p12 = (args.length >= 3) ? new File(args[2]) : null;

        SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");

        if (p12 != null) {
            KeyStore ks = KeyStore.getInstance("pkcs12");
            InputStream in = new java.io.FileInputStream(p12);
            ks.load(in, "secret".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "secret".toCharArray());
            sslContext.init(kmf.getKeyManagers(), tm, null);
        } else {
            sslContext.init(null, tm, null);
        }
        connect(url, ua, sslContext);

    }

    static void connect(URL url, String name, SSLContext sslContext) throws IOException {
       HttpsURLConnection scon = (HttpsURLConnection)url.openConnection();
       scon.setSSLSocketFactory(sslContext.getSocketFactory());
       scon.setRequestProperty("Content-Type","text/html");
       scon.setRequestProperty("User-Agent" , name);
       scon.setRequestMethod("GET");
       scon.connect();
       System.out.println("response code = "+scon.getResponseCode());
       for (Map.Entry<String,List<String>> hf: scon.getHeaderFields().entrySet()) {
         String key = hf.getKey();
           for (String val: hf.getValue()) {
               System.out.println(key+": "+val);
           }
       }
       System.out.println();
       InputStream in = scon.getInputStream();
       byte[] buf = new byte[1024];
       int read;
       while((read=in.read(buf)) >= 0) {
           System.out.println(new String(buf,0,read));
       }
    }

    static X509TrustManager trustAll = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0];  }
    };
    static X509TrustManager[] tm = { trustAll };

    private static void usage() {
        System.out.println("JavaTLSBugTest url [ua] [p12File]");
        System.out.println(" url: URL to a service requesting a client certificate");
        System.out.println(" ua: user agent, or else default Java UA 'Java/1.7.0'");
        System.out.println(" p12File: path to p12 file containing a client certificate and corresponding password");
        System.out.println();
        System.out.println(" examples:");
        System.out.println(" $ JavaTLSBugTest https://localhost:8443/test/WebId Opera client.p12");
        System.out.println(" $ JavaTLSBugTest https://localhost:8443/test/WebId IE");
        System.exit(-1);
    }

}
