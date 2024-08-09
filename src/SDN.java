import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SDN {
	 private static final Pattern imo_pattern = Pattern.compile("IMO (\\d{7})");

    public static final String input_url = "https://www.treasury.gov/ofac/downloads/sdn.csv";
    public static final String temp_file = "sdn.csv";
    public static final String output_file = "Parsed_SDN.csv";
    public static final String output_imo ="IMO_SDN.csv";
    
    

    public static void main(String[] args) {
        try {
            setupSslContext();

            
            downloadFile(input_url, temp_file);

            processCSVIMO(temp_file,output_imo);
            processCSV(temp_file, output_file);

            System.out.println("Processing complete. Output written to " + output_file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setupSslContext() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadFile(String urlString, String outputFileName) throws IOException {
        URI uri;
        try {
            uri = new URI(urlString);
            URL url = uri.toURL();
            try (BufferedReader in = new BufferedReader(new java.io.InputStreamReader(url.openStream()));
                 FileWriter out = new FileWriter(outputFileName)) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line + "\n");
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void processCSV(String inputFile, String outputFile) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(inputFile));
             CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {

            String[] newHeader = {"File", "Type", "Name", "Alias", "Country"};
            writer.writeNext(newHeader);

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 4) continue;
                
               // System.out.println("Processing row: " + String.join(", ", row));
                
                String type = row[2].trim();
                String name = escapeCsv(row[1].trim());
                String country = escapeCsv(row[3].trim());

                String typeLabel;
                if (type.equals("-0-") || type.isEmpty()) {
                    typeLabel = "Entity";
                } else if (type.equalsIgnoreCase("individual") || type.equalsIgnoreCase("vessel")) {
                    typeLabel = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase(); 
                } else {
                    continue; 
                }

                writer.writeNext(new String[]{"SDN", typeLabel, name, "null", country});
            }
        } catch (CsvValidationException e) {
            e.printStackTrace();
        }
    }
    public static void processCSVIMO(String inputFile, String outputFile) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(inputFile));
             CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {

            // Write headers
            String[] newHeader = {"File", "IMOnum", "Name"};
            writer.writeNext(newHeader);

            String[] row;
            while ((row = reader.readNext()) != null) {
                System.out.println("Processing row: " + String.join(", ", row));

                if (row.length < 12) {
                    System.out.println("Skipping row due to insufficient columns: " + String.join(", ", row));
                    continue;
                }

                String imoText = row.length > 11 ? row[11] : "";
                String name = row.length > 1 ? row[1].trim() : "";

                String imo = extractIMO(imoText);
                if (imo != null) {
                  //  System.out.println("Valid IMO number found: " + imo);
                    writer.writeNext(new String[]{"SDN", imo, escapeCsv(name)});
                } else {
                  //  System.out.println("No valid IMO number found in: " + imoText);
                }
            }
        } catch (CsvValidationException e) {
            e.printStackTrace();
        }
    }
    
    private static String extractIMO(String text) {
        Matcher matcher = imo_pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1); 
        } else {
            return null; 
        }
    }

    

    private static String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "").replace(",", "").replace("[", "").replace("]", "");
    }

}
