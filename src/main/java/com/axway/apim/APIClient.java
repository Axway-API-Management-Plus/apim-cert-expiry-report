package com.axway.apim;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class APIClient {


    public HttpClient createConnection() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, (x509CertChain, authType) -> true);
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);
        CloseableHttpClient httpclient = HttpClientBuilder.create().disableRedirectHandling().setSSLSocketFactory(sslConnectionSocketFactory)
                .setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
        return httpclient;
    }

    public void closeConnection(HttpClient httpClient) {
        HttpClientUtils.closeQuietly(httpClient);
    }


}
