package com.axway.apim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.ServerException;
import java.util.List;

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

    public List<FrontendAPI> listAPIs() throws IOException,
            UnsupportedOperationException, APIMException, URISyntaxException {
        HttpResponse httpResponse = null;
        try {
            HttpUriRequest getBackend = RequestBuilder.get().setUri(new URI(url + API_BASEPATH + "/proxies"))
                    .addHeader("Authorization", basicAuthCred).build();
            httpResponse = httpClient.execute(getBackend);
            check200(httpResponse, " API not found");
            return objectMapper.readValue(httpResponse.getEntity().getContent(),
                    new TypeReference<List<FrontendAPI>>() {
                    });
        } catch (PathNotFoundException e) {
            throw new ServerException("API not found", e);
        } finally {
            HttpClientUtils.closeQuietly(httpResponse);
        }

    }


    public String getOrgName(String orgId) throws URISyntaxException, IOException, UnsupportedOperationException {
        HttpResponse httpResponse = null;
        try {
            HttpUriRequest orgURL = RequestBuilder.get().setUri(new URI(url + API_BASEPATH + "/organizations/" + orgId)).
                    addHeader("Authorization", basicAuthCred).build();
            httpResponse = httpClient.execute(orgURL);
            DocumentContext documentContext = JsonPath.parse(httpResponse.getEntity().getContent());
            return documentContext.read("$.name", String.class);
        } finally {
            HttpClientUtils.closeQuietly(httpResponse);
        }
    }

    public User getUsername(String userId) throws URISyntaxException, IOException, UnsupportedOperationException {
        HttpResponse httpResponse = null;
        try {
            HttpUriRequest userURL = RequestBuilder.get().setUri(new URI(url + API_BASEPATH + "/users/" + userId)).
                    addHeader("Authorization", basicAuthCred).build();
            httpResponse = httpClient.execute(userURL);
            return objectMapper.readValue(httpResponse.getEntity().getContent(), User.class);
        } finally {
            HttpClientUtils.closeQuietly(httpResponse);
        }
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
