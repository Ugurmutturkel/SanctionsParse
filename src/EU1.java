import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.security.cert.X509Certificate;

public class EU1 {

    public static final String BASE_URL = "https://www.sanctionsmap.eu/api/v1/travelbans/file/";

    public static void main(String[] args) {
        try {
            disableSSLVerification();
            int startVersion = 79;
            int increment = 2;
            int currentVersion = startVersion;
            int lastSuccessfulVersion = startVersion;

            while (true) {
                String xmlUrl = BASE_URL + currentVersion;
                System.out.println("Trying URL: " + xmlUrl);

                if (parseXMLAndWriteCSV(xmlUrl, "parsed_EU1.csv")) {
                    lastSuccessfulVersion = currentVersion; 
                    currentVersion += increment; 
                } else {
                    System.out.println("Encountered error with URL: " + xmlUrl);
                    break;
                }
            }

            if (currentVersion > startVersion) {
                String lastSuccessfulUrl = BASE_URL + lastSuccessfulVersion;
                System.out.println("Reverting to last successful URL: " + lastSuccessfulUrl);
                parseXMLAndWriteCSV(lastSuccessfulUrl, "parsed_EU1.csv");
            }

            System.out.println("Process completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean parseXMLAndWriteCSV(String xmlUrl, String csvFilePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            URI uri = new URI(xmlUrl);
            URL url = uri.toURL();
            try (InputStream inputStream = url.openStream()) {
                Document document = builder.parse(inputStream);
                document.getDocumentElement().normalize();

                String typeCode = "";
                NodeList subjectTypeNodes = document.getElementsByTagName("subjectType");
                if (subjectTypeNodes.getLength() > 0) {
                    Element subjectTypeElement = (Element) subjectTypeNodes.item(0);
                    typeCode = subjectTypeElement.getAttribute("code").replace("\"", "");
                    if ("person".equals(typeCode)) {
                        typeCode = "Individual"; 
                    }
                }

                NodeList sanctionEntityNodes = document.getElementsByTagName("sanctionEntity");

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath));
                     CSVPrinter csvPrinter = new CSVPrinter(writer,
                         CSVFormat.DEFAULT.builder()
                             .setHeader("File", "Type", "Name", "Alias", "Country")
                             .build())) {

                    for (int i = 0; i < sanctionEntityNodes.getLength(); i++) {
                        Node sanctionEntityNode = sanctionEntityNodes.item(i);

                        if (sanctionEntityNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element sanctionEntityElement = (Element) sanctionEntityNode;
                            NodeList nameAliasNodes = sanctionEntityElement.getElementsByTagName("nameAlias");
                            NodeList birthdateNodes = sanctionEntityElement.getElementsByTagName("birthdate");

                            String primaryName = null;
                            String countryDescription = "null";
                            boolean hasAliases = false;

                            for (int j = 0; j < nameAliasNodes.getLength(); j++) {
                                Node nameAliasNode = nameAliasNodes.item(j);

                                if (nameAliasNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element nameAliasElement = (Element) nameAliasNode;
                                    String wholeName = nameAliasElement.getAttribute("wholeName").replace("\"", "").replaceAll("[\u0600-\u06FF]", "").trim();

                                    if (j < birthdateNodes.getLength()) {
                                        Element birthdateElement = (Element) birthdateNodes.item(j);
                                        if (birthdateElement != null && birthdateElement.hasAttribute("countryDescription")) {
                                            countryDescription = birthdateElement.getAttribute("countryDescription").replace("\"", "").trim();
                                            if (countryDescription == null || countryDescription.isEmpty() || "UNKNOWN".equals(countryDescription)) {
                                                countryDescription = "null";
                                            }
                                        }
                                    }

                                    if (wholeName.isEmpty() || wholeName.equals(" ")) {
                                        continue;
                                    }

                                    if (primaryName == null) {
                                        primaryName = wholeName;
                                    } else {
                                        csvPrinter.printRecord("EU1", typeCode, primaryName, wholeName, countryDescription);
                                        hasAliases = true;
                                    }
                                }
                            }

                            if (primaryName != null) {
                                if (!hasAliases) {
                                    csvPrinter.printRecord("EU1", typeCode, primaryName, "No Alias", countryDescription);
                                }
                            }
                        }
                    }
                }

                System.out.println("CSV file created successfully for URL: " + xmlUrl);
                return true; 
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false; 
        }
    }

    public static void disableSSLVerification() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }
}
