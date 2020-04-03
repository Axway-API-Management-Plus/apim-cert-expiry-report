package com.axway.apim;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Base64;
import java.util.concurrent.Callable;

@picocli.CommandLine.Command(description = "AMPLIFY APIM V7 Certificate Expiry  Checker",
        name = "checkCertificateExpiry", mixinStandardHelpOptions = true, version = "1.0")
public class App implements Callable<Integer> {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    @picocli.CommandLine.Option(required = true, names = {"-u", "--username"}, description = "APIManager Username ")
    private String username;

    @picocli.CommandLine.Option(required = true, names = {"-p", "--password"}, description = "APIManager password")
    private String password;

    @picocli.CommandLine.Option(names = {"-s", "--serverURL"}, description = "APIMAnager URL")
    private String url;



    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        HttpClient httpClient = null;
        APIClient apiClient = new APIClient();

        APIManagerClient apiManagerClient = null;
        try {
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            httpClient = apiClient.createConnection();
            apiManagerClient = new APIManagerClient(httpClient, url, authHeader);

            apiManagerClient.listAPIs();
        } catch (UnsupportedOperationException  e) {
            logger.error("Error : {}", e);
            return 1;
        } finally {

            if (httpClient != null) {
                apiClient.closeConnection(httpClient);
            }
        }
        return 0;

    }
}
