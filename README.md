# API manager Certificate Expiry Report

## Build project

```bash
$mvn package
```

mvn package command creates a jar file named target/apimanager-cert-expiry-check-1.0.1.jar

## Usage

```bash
$java -jar target/apimanager-cert-expiry-check-1.0.1.jar
Usage: checkCertificateExpiry [-hV] -d=<daysBeforeExpires> -p=<password>
                              -s=<url> -u=<username>
AMPLIFY APIM V7 Certificate Expiry  Checker
  -d, --daysBeforeExpires=<daysBeforeExpires>
                          Days Before Expires
  -h, --help              Show this help message and exit.
  -p, --password=<password>
                          APIManager password
  -s, --serverURL=<url>   API Manager URL
  -u, --username=<username>
                          APIManager Username
  -V, --version           Print version information and exit.
```
## Create certificate expiry report

```bash
$java -jar target/apimanager-cert-expiry-check-1.0.1.jar -u apiadmin -p changeme -s https://10.129.59.81:8075 -d 7
```

The script creates a csv file named output.csv in a current directory if there are expired certificate on API manger. (To test the functionality change your system timezone to future date )

## Sample output.csv

![output.csv](output.png)

```html
Index,API Name,API Version,Developer Org name,Developer login name,Developer email,Expired Certificate DN,Expired Certificate Alias Name,Expired Date
1,Swagger Petstore,1.0.5,API Development,apiadmin,apiadmin@localhost,"CN=*.swagger.io","CN=*.swagger.io",Sat May 15 05:00:00 MST 2021
2,Swagger Petstore,1.0.5,API Development,apiadmin,apiadmin@localhost,"CN=Amazon, OU=Server CA 1B, O=Amazon, C=US","CN=Amazon, OU=Server CA 1B, O=Amazon, C=US",Sat Oct 18 17:00:00 MST 2025
```

