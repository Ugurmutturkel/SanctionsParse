import com.opencsv.CSVReader;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class USA {

    public static void downloadFile(String fileUrl, String fileName) {
        try {
            URI uri = new URI(fileUrl);
            URL url = uri.toURL();
            try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
                byte dataBuffer[] = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseCSVFile(String fileName, List<String> countries, List<String> types, List<String> names, List<List<String>> altNames) {
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            String[] headers;
            try {
                headers = reader.readNext();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (headers == null) {
                System.out.println("Empty CSV file");
                return;
            }

            int typeIndex = -1;
            int nameIndex = -1;
            int altNameIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                if (headers[i].equalsIgnoreCase("type")) {
                    typeIndex = i;
                } else if (headers[i].equalsIgnoreCase("name")) {
                    nameIndex = i;
                } else if (headers[i].equalsIgnoreCase("alt_names")) {
                    altNameIndex = i;
                }
            }

            if (typeIndex == -1 || nameIndex == -1 || altNameIndex == -1) {
                System.out.println("\"type\", \"name\", or \"alt_names\" column not found");
                return;
            }

            String[] fields;
            while (true) {
                try {
                    fields = reader.readNext();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                if (fields == null) {
                    break;
                }

                if (fields.length > typeIndex && fields.length > nameIndex && fields.length > altNameIndex) {
                    String typeValue = removeURLsAndQuotes(fields[typeIndex]).trim();
                    if (typeValue.isEmpty()) {
                        continue;  
                    }

                    countries.add("USA");
                    types.add(typeValue);
                    names.add(removeURLsAndQuotes(fields[nameIndex]));
                    List<String> altNamesList = new ArrayList<>();
                    String[] altNamesArray = removeURLsAndQuotes(fields[altNameIndex]).split(";");
                    for (String altName : altNamesArray) {
                        altNamesList.add(altName.trim());
                    }
                    altNames.add(altNamesList);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        
    public static void parseCSVIMO(String fileName, List<String> names, List<String> imoNumbers) {
        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            String[] headers;
            try {
                headers = reader.readNext();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (headers == null) {
                System.out.println("Empty CSV file");
                return;
            }

            int typeIndex = -1;
            int nameIndex = -1;
            int idsIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                if (headers[i].equalsIgnoreCase("type")) {
                    typeIndex = i;
                } else if (headers[i].equalsIgnoreCase("name")) {
                    nameIndex = i;
                } else if (headers[i].equalsIgnoreCase("ids")) { 
                    idsIndex = i;
                }
            }

            if (typeIndex == -1 || nameIndex == -1 || idsIndex == -1) {
                System.out.println("\"type\", \"name\", or \"ids\" column not found");
                return;
            }

            String[] fields;
            while (true) {
                try {
                    fields = reader.readNext();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                if (fields == null) {
                    break;
                }

                if (fields.length > typeIndex && fields.length > nameIndex && fields.length > idsIndex) {
                    String typeValue = removeURLsAndQuotes(fields[typeIndex]).trim();
                    if (typeValue.equalsIgnoreCase("vessel")) {
                        names.add(removeURLsAndQuotes(fields[nameIndex]));
                        imoNumbers.add(extractIMONumber(fields[idsIndex]));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void writeCSVIMO(String fileName, List<String> imoNumbers, List<String> names) {
        try (FileWriter writer = new FileWriter(fileName, StandardCharsets.UTF_8)) {
            writer.write("File,IMOnum,Name\n");

            for (int i = 0; i < imoNumbers.size(); i++) {
                String imoNumber = imoNumbers.get(i);
                String name = names.get(i);

                if (imoNumber == null || imoNumber.trim().isEmpty()) {
                    imoNumber = "null";
                }

                writer.write(String.join(",", escapeCsv("USA"), escapeCsv(imoNumber), escapeCsv(name)));
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String extractIMONumber(String idsText) {
        if (idsText == null || idsText.isEmpty()) {
            return "";
        }

        Pattern pattern = Pattern.compile("IMO (\\d{7})");
        Matcher matcher = pattern.matcher(idsText);

        if (matcher.find()) {
            return matcher.group(1); 
        }
        return "";
    }

    
    public static String removeURLsAndQuotes(String text) {
        return text.replaceAll("http[s]?://\\S+", "").replace("\"", "").trim();
    }

    public static void writeCSVFile(String fileName, List<String> countries, List<String> types, List<String> names, List<List<String>> altNames) {
        try (FileWriter writer = new FileWriter(fileName, StandardCharsets.UTF_8)) {
            writer.write("File,Type,Name,Alias,Country\n");

            for (int i = 0; i < countries.size(); i++) {
                String type = types.get(i);
                String name = names.get(i);
                List<String> altNameList = altNames.get(i);

                if (altNameList.isEmpty() || (altNameList.size() == 1 && altNameList.get(0).isEmpty())) {
                    writer.write(String.join(",", escapeCsv("USA"), escapeCsv(type), escapeCsv(name), "null", escapeCsv("null")));
                    writer.write("\n");
                } else {
                    for (String alias : altNameList) {
                        writer.write(String.join(",", escapeCsv("USA"), escapeCsv(type), escapeCsv(name), escapeCsv(alias), escapeCsv("null")));
                        writer.write("\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace(",", "");
    }

    public static void main(String[] args) {
        String fileUrl = "https://data.trade.gov/downloadable_consolidated_screening_list/v1/consolidated.csv";
        String inputFileName = "consolidated.csv";
        String outputFileName = "parsed_USA.csv";

        System.out.println("Downloading File...");
        downloadFile(fileUrl, inputFileName);

        List<String> countries = new ArrayList<>();
        List<String> types = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<List<String>> altNames = new ArrayList<>();

        System.out.println("Parsing CSV File...");
        parseCSVFile(inputFileName, countries, types, names, altNames);

        System.out.println("Writing CSV File...");
        writeCSVFile(outputFileName, countries, types, names, altNames);
        System.out.println("Done");
        //IMO part
        List<String> namesimo = new ArrayList<>();
        List<String> imoNumbers = new ArrayList<>();

        System.out.println("Parsing CSV File...");
        parseCSVIMO(inputFileName, namesimo, imoNumbers);
        String outputFileNameimo = "IMO_USA.csv";
        writeCSVIMO(outputFileNameimo, imoNumbers, namesimo);
    }
}
