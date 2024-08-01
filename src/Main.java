import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.BufferedInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Main {

    public static void main(String[] args) {
    	callUSA();
    	callUK();
    	callUN();
    	callEU1();
    	callEU2();
        String[] inputFiles = {
            "parsed_USA.csv",
            "parsed_UK.csv",
            "parsed_UN.csv",
            "parsed_EU1.csv",
            "parsed_EU2.csv"
        };
        String outputFile = "SanctionsParsed.csv";

        combineCSVFiles(inputFiles, outputFile);
    }

    public static void combineCSVFiles(String[] inputFiles, String outputFile) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            boolean isFirstFile = true;
            for (String inputFile : inputFiles) {
                try (CSVReader reader = new CSVReader(new FileReader(inputFile))) {
                    List<String[]> allRows;
                    try {
                        allRows = reader.readAll();
                    } catch (CsvException e) {
                        System.err.println("Error parsing CSV file " + inputFile + ": " + e.getMessage());
                        continue; 
                    }

                    if (allRows.isEmpty()) {
                        System.out.println("File " + inputFile + " is empty.");
                        continue;
                    }

                    if (isFirstFile) {
                        writer.writeAll(allRows);
                        isFirstFile = false;
                    } else {
                        writer.writeAll(allRows.subList(1, allRows.size()));
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file " + inputFile + ": " + e.getMessage());
                }
            }
            System.out.println("CSV files combined successfully into " + outputFile);
        } catch (IOException e) {
            System.err.println("Error writing file " + outputFile + ": " + e.getMessage());
        }
    }
    public static void callUSA(){
    	 String fileUrlUSA = "https://data.trade.gov/downloadable_consolidated_screening_list/v1/consolidated.csv";
         String inputFileNameUSA = "consolidated.csv";
         String outputFileNameUSA = "parsed_USA.csv";

         USA.downloadFile(fileUrlUSA, inputFileNameUSA);

         List<String> countries = new ArrayList<>();
         List<String> types = new ArrayList<>();
         List<String> names = new ArrayList<>();
         List<List<String>> altNames = new ArrayList<>();

         USA.parseCSVFile(inputFileNameUSA, countries, types, names, altNames);

         USA.writeCSVFile(outputFileNameUSA, countries, types, names, altNames);
         System.out.println("USA Done");
    }
    public static void callUK() {
        String urlStringUK = "https://docs.fcdo.gov.uk/docs/UK-Sanctions-List.html";
        String csvFilePathUK = "parsed_UK.csv";

        try {
            Document document = Jsoup.connect(urlStringUK).get();
            UK.writeToCsv(document, csvFilePathUK);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("UK Done");
    }
    public static void callUN() {
        String urlStringUN = "https://scsanctions.un.org/consolidated";
        String csvFilePathUN = "parsed_UN.csv";

        try {
            Document document = Jsoup.connect(urlStringUN).get();
            UN.writeToCsv(document, csvFilePathUN);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("UN Done");
    }
    public static void callEU1() {
    	
    	 try {
             EU1.disableSSLVerification();
             int startVersion = 79;
             int increment = 2;
             int currentVersion = startVersion;
             int lastSuccessfulVersion = startVersion;

             while (true) {
                 String xmlUrl = EU1.BASE_URL + currentVersion;
                 System.out.println("Trying URL: " + xmlUrl);

                 if (EU1.parseXMLAndWriteCSV(xmlUrl, "parsed_EU1.csv")) {
                     lastSuccessfulVersion = currentVersion; 
                     currentVersion += increment; 
                 } else {
                     System.out.println("Encountered error with URL: " + xmlUrl);
                     break;
                 }
             }

             if (currentVersion > startVersion) {
                 String lastSuccessfulUrl = EU1.BASE_URL + lastSuccessfulVersion;
                 System.out.println("Reverting to last successful URL: " + lastSuccessfulUrl);
                 EU1.parseXMLAndWriteCSV(lastSuccessfulUrl, "parsed_EU1.csv");
             }

             System.out.println("Process completed.");
         } catch (Exception e) {
             e.printStackTrace();
         }
    	 System.out.println("EU1 Done");
    }
    public static void callEU2() {
    	String urlStringEU2 = "https://data.europa.eu/api/hub/search/datasets/consolidated-list-of-persons-groups-and-entities-subject-to-eu-financial-sanctions";

        try {
            URI uri = new URI(urlStringEU2);
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
                                    EU2.downloadFile(accessUrl, fileName);

                                    String newFileName = "parsed_EU2.csv";
                                    EU2.parseXMLAndWriteCSV(fileName, newFileName);
                                    System.out.println("EU2 Done");

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
}
