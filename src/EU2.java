import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class EU2 {

    public static void main(String[] args) {
        String urlString = "https://data.europa.eu/api/hub/search/datasets/consolidated-list-of-persons-groups-and-entities-subject-to-eu-financial-sanctions";

        try {
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                response.append(new String(buffer, 0, bytesRead));
            }
            in.close();

            String jsonResponse = response.toString();
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if (jsonObject.has("result")) {
                JSONObject result = jsonObject.getJSONObject("result");
                if (result.has("distributions")) {
                    JSONArray distributions = result.getJSONArray("distributions");
                    for (int j = 0; j < distributions.length(); j++) {
                        JSONObject distribution = distributions.getJSONObject(j);
                        if (distribution.has("access_url")) {
                            JSONArray accessUrls = distribution.getJSONArray("access_url");
                            for (int k = 0; k < accessUrls.length(); k++) {
                                String accessUrl = accessUrls.getString(k);
                                if (accessUrl.contains("xmlFullSanctionsList")) {
                                    System.out.println("Access URL: " + accessUrl);

                                    String fileName = "EU2_sanctionslist.xml";
                                    downloadFile(accessUrl, fileName);

                                    String newFileName = "parsed_EU2.csv";
                                    parseXMLAndWriteCSV(fileName, newFileName);

                                    return;
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("Result not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   

    public static void downloadFile(String fileUrl, String fileName) {
        try {
            URI uri = new URI(fileUrl);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream in = new BufferedInputStream(connection.getInputStream());
                     OutputStream out = new FileOutputStream(fileName)) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    System.out.println("File downloaded: " + fileName);
                }
            } else {
                System.out.println("Failed to download file. HTTP response code: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean parseXMLAndWriteCSV(String xmlFilePath, String csvFilePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            File xmlFile = new File(xmlFilePath);
            Document document = builder.parse(xmlFile);
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
                        NodeList birthdateNodes = document.getElementsByTagName("birthdate");

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
                                    csvPrinter.printRecord("EU2", typeCode, primaryName, wholeName, countryDescription);
                                    hasAliases = true;
                                }
                            }
                        }

                        if (primaryName != null) {
                            if (!hasAliases) {
                                csvPrinter.printRecord("EU2", typeCode, primaryName, "null", countryDescription);
                            }
                        }
                    }
                }
            }

            System.out.println("CSV file created successfully for XML file: " + xmlFilePath);
            return true; 
        } catch (Exception e) {
            e.printStackTrace();
            return false; 
        }
    }
}
