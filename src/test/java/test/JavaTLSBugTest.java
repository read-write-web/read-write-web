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
