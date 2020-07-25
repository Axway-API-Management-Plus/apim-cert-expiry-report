package com.axway.apim;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

@picocli.CommandLine.Command(description = "AMPLIFY APIM V7 Certificate Expiry  Checker",
        name = "checkCertificateExpiry", mixinStandardHelpOptions = true, version = "1.0")
public class App implements Callable<Integer> {

    private static String NEW_LINE = "\r\n";
    private static String DOUBLE_QUOTE = "\"";
    private static Logger logger = LoggerFactory.getLogger(App.class);

    @picocli.CommandLine.Option(required = true, names = {"-u", "--username"}, description = "APIManager Username ")
    private String username;

    @picocli.CommandLine.Option(required = true, names = {"-p", "--password"}, description = "APIManager password")
    private String password;

    @picocli.CommandLine.Option(names = {"-s", "--serverURL"}, description = "API Manager URL")
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
        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        try {
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            httpClient = apiClient.createConnection();
            apiManagerClient = new APIManagerClient(httpClient, url, authHeader);
            List<FrontendAPI> frontendAPIs = apiManagerClient.listAPIs();
            if (frontendAPIs.isEmpty()) {
                logger.info("No API found");
                return 1;
            }
            int index = 1;
            for (FrontendAPI frontendAPI : frontendAPIs) {
                String orgName = apiManagerClient.getOrgName(frontendAPI.getOrganizationId());
                logger.info(orgName);
                User user = apiManagerClient.getUsername(frontendAPI.getCreatedBy());
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
                        if (fileWriter == null) {
                            fileWriter = new FileWriter("output.csv");
                            bufferedWriter = new BufferedWriter(fileWriter);
                            bufferedWriter.write("Index" + "," + "API Name" + "," + "API Version" + "," + "Developer Org name" +
                                    "," + "Developer login name" + "," + "Developer email" + "," + "Expired Certificate DN" + "," +
                                    "Expired Certificate Alias Name" + "," + "Expired Date");
                            bufferedWriter.write(NEW_LINE);
                        }
                        StringBuffer stringBuffer = new StringBuffer();
                        stringBuffer.append(index);
                        stringBuffer.append(",");
                        stringBuffer.append(frontendAPI.getName());
                        stringBuffer.append(",");
                        stringBuffer.append(frontendAPI.getVersion());
                        stringBuffer.append(",");
                        stringBuffer.append(orgName);
                        stringBuffer.append(",");
                        stringBuffer.append(user.getLoginName());
                        stringBuffer.append(",");
                        stringBuffer.append(user.getEmail());
                        stringBuffer.append(",");
                        stringBuffer.append(DOUBLE_QUOTE);
                        stringBuffer.append(caCert.getSubject());
                        stringBuffer.append(DOUBLE_QUOTE);
                        stringBuffer.append(",");
                        stringBuffer.append(DOUBLE_QUOTE);
                        stringBuffer.append(caCert.getAlias());
                        stringBuffer.append(DOUBLE_QUOTE);
                        stringBuffer.append(",");
                        stringBuffer.append(certExpiryDate.toString());
                        bufferedWriter.write(stringBuffer.toString());
                        bufferedWriter.write(NEW_LINE);
                        index += index;
                    }
                }
            }
        } catch (UnsupportedOperationException e) {
            logger.error("Error : {}", e);
            return 1;
        } finally {

            if (httpClient != null) {
                apiClient.closeConnection(httpClient);
            }

            if (bufferedWriter != null) {
                bufferedWriter.close();
            }

            if (fileWriter != null) {
                fileWriter.close();
            }
        }
        return 0;

    }
}
