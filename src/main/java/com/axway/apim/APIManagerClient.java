package com.axway.apim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.cert.CertificateException;
import javax.security.cert.CertificateExpiredException;
import javax.security.cert.CertificateNotYetValidException;
import javax.security.cert.X509Certificate;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.ServerException;
import java.security.PublicKey;
import java.util.*;

public class APIManagerClient {


    private final String API_BASEPATH = "/api/portal/v1.3/";
    private static Logger logger = LoggerFactory.getLogger(APIManagerClient.class);

    private HttpClient httpClient;
    private String url;
    private String basicAuthCred;

    private ObjectMapper objectMapper = new ObjectMapper();

    public APIManagerClient(HttpClient httpClient, String url, String basicAuthCred) {
        this.httpClient = httpClient;
        this.url = url;
        this.basicAuthCred = basicAuthCred;
    }

    public String listAPIs() throws IOException,
            UnsupportedOperationException, APIMException, URISyntaxException {
        HttpResponse httpResponse = null;
        try {

            HttpUriRequest getBackend = RequestBuilder.get().setUri(new URI(url + API_BASEPATH + "/proxies"))
                    .addHeader("Authorization", basicAuthCred).build();
            httpResponse = httpClient.execute(getBackend);
            check200(httpResponse, " API not found");
            List<FrontendAPI> frontendAPIS = objectMapper.readValue(httpResponse.getEntity().getContent(),
                    new TypeReference<List<FrontendAPI>>() {
                    });


            if (frontendAPIS.isEmpty()) {
                logger.info("No API found");
                return null;
            }

            for (FrontendAPI frontendAPI : frontendAPIS) {

                String orgName = getOrgName(frontendAPI.getOrganizationId());
                logger.info(orgName);
                User user = getUsername(frontendAPI.getCreatedBy());
                logger.info(user.getLoginName() + user.getEmail());
                List<CACert> certs = frontendAPI.getCaCerts();
                for (CACert caCert : certs) {
                    logger.info(caCert.getAlias());

                    Date currentDate = Calendar.getInstance().getTime();

                    Date certExpiryDate = new Date();
                    certExpiryDate.setTime(caCert.getNotValidAfter());
                    logger.info("Expiry Date : {}", certExpiryDate.toString());
                    if (currentDate.after(certExpiryDate)) {
                        logger.info("Cert Expired");
                    }
                }
            }

        } catch (PathNotFoundException e) {
            throw new ServerException("API not found", e);
        } finally {
            HttpClientUtils.closeQuietly(httpResponse);
        }
        return null;

    }


    public String getOrgName(String orgId) throws URISyntaxException, ClientProtocolException,
            IOException, UnsupportedOperationException, ServerException {
        HttpResponse httpResponse = null;
        try {
            HttpUriRequest orgURL = RequestBuilder.get().setUri(new URI(url + API_BASEPATH + "/organizations/" + orgId)).addHeader("Authorization", basicAuthCred).build();
            httpResponse = httpClient.execute(orgURL);
            DocumentContext documentContext = JsonPath.parse(httpResponse.getEntity().getContent());
            String orgName = documentContext.read("$.name", String.class);
            return orgName;

        } finally {
            HttpClientUtils.closeQuietly(httpResponse);
        }
    }

    public User getUsername(String userId) throws URISyntaxException, ClientProtocolException,
            IOException, UnsupportedOperationException, ServerException {
        HttpResponse httpResponse = null;
        try {
            HttpUriRequest userURL = RequestBuilder.get().setUri(new URI(url + API_BASEPATH + "/users/" + userId)).addHeader("Authorization", basicAuthCred).build();
            httpResponse = httpClient.execute(userURL);

            User user = objectMapper.readValue(httpResponse.getEntity().getContent(), User.class);
            return user;

        } finally {
            HttpClientUtils.closeQuietly(httpResponse);
        }
    }


    private List<CACert> getCerts(File certDir) throws FileNotFoundException, CertificateException {

        List<CACert> caCerts = new ArrayList<>();

        File[] files = certDir.listFiles();
        for (File file : files) {
            CACert caCert = new CACert();
            InputStream inputStream = new FileInputStream(file);
            X509Certificate certificate = X509Certificate.getInstance(inputStream);
            String issuer = certificate.getIssuerDN().getName();
            String dn = certificate.getSubjectDN().getName();
            Date validFrom = certificate.getNotBefore();
            Date validTo = certificate.getNotAfter();
            int version = certificate.getVersion();
            byte[] encodedData = certificate.getEncoded();
            PublicKey publicKey = certificate.getPublicKey();
            String algorithm = publicKey.getAlgorithm();
            caCert.setCertBlob(Base64.getEncoder().encodeToString(certificate.getEncoded()));
            caCert.setName(dn);
            caCert.setAlias(dn);
            caCert.setSubject(dn);
            caCert.setIssuer(issuer);
            caCert.setVersion(version);
            caCert.setNotValidBefore(validFrom.getTime());
            caCert.setNotValidAfter(validTo.getTime());
            caCert.setSignatureAlgorithm(algorithm);

            String sha1Fingerprint = Hex.encodeHexString(DigestUtils.sha1(encodedData));
            String md5Fingerprint = Hex.encodeHexString(DigestUtils.md5(encodedData));

            caCert.setSha1Fingerprint(sha1Fingerprint);
            caCert.setMd5Fingerprint(md5Fingerprint);

            caCerts.add(caCert);

            try {
                certificate.checkValidity();
            } catch (CertificateExpiredException e) {
                caCert.setExpired(true);
            } catch (CertificateNotYetValidException e) {
                caCert.setNotYetValid(true);
            }

            caCert.setOutbound(true);
        }

        return caCerts;

    }


    private void check200(HttpResponse response, String customErrorMessage)
            throws ParseException, IOException, APIMException {

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode != 200) {
            String errorMsg = customErrorMessage + " Status code " + statusCode + " Reason "
                    + statusLine.getReasonPhrase();
            logger.error(errorMsg);
            logger.error("Server Response {}", EntityUtils.toString(response.getEntity()));
            throw new APIMException(errorMsg);
        }
    }


}
