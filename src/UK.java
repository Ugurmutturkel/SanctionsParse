import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class UK {

    public static void main(String[] args) {
        String urlString = "https://docs.fcdo.gov.uk/docs/UK-Sanctions-List.html";
        String csvFilePath = "parsed_UK.csv";
        String csvFilePathIMO = "IMO_UK.csv";

        try {
            Document document = Jsoup.connect(urlString).get();
            writeToCsvIMO(document,csvFilePathIMO);
            writeToCsv(document, csvFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeToCsv(Document document, String csvFilePath) {
        try {
            File csvFile = new File(csvFilePath); 
            boolean fileExists = csvFile.exists();

            System.out.println("CSV file path: " + csvFilePath);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, false))) { // Set append to false
                
                if (!fileExists) {
                    writer.write("File,Type,Primary Name,Alias,Country");
                    writer.newLine();
                }

                Elements entries = document.select("div > div");

                System.out.println("Number of entries found: " + entries.size());

                String type = "";
                String country = "";

                for (Element entry : entries) {
                    
                    Elements typeElements = entry.select("b:contains(Unique ID:) + span, span:contains(- Entity), span:contains(- Individual)");
                    if (typeElements.size() >= 2) {
                        type = typeElements.get(1).text().trim();

                        type = type.replace("- Entity", "Entity").replace("- Individual", "Individual").trim();

                        if (type.toLowerCase().contains("regulations")) {
                            continue;
                        }
                    }

                    Elements regimeElements = entry.select("b:contains(Regime Name:) + span");
                    if (!regimeElements.isEmpty()) {
                        country = regimeElements.first().text().trim();
                    } else {
                        country = "null";
                    }

                    Elements nameEntries = entry.select("div:has(b:contains(Name:))");
                    String primaryName = "";
                    StringBuilder aliases = new StringBuilder();

                    for (Element nameEntry : nameEntries) {
                        Elements names = nameEntry.select("b:contains(Name:) + span");
                        Elements nameTypes = nameEntry.select("b:contains(Name Type:) + span");

                        int numNames = Math.min(names.size(), nameTypes.size());

                        for (int i = 0; i < numNames; i++) {
                            String name = names.get(i).text();
                            String nameType = nameTypes.get(i).text();

                            if (name.toLowerCase().contains("(sanctions)")) {
                                continue;
                            }

                            if (name.toLowerCase().contains("regulations")) {
                                continue;
                            }
                            
                            if (nameType.equalsIgnoreCase("Primary Name")) {
                                primaryName = name;
                            } else {
                                if (aliases.length() > 0) {
                                    aliases.append("; ");
                                }
                                aliases.append(name);
                            }
                        }
                    }
                    for (Element nameEntry : nameEntries) {
                        Elements nameElements = nameEntry.select("b:contains(Name:)");

                        for (Element nameElement : nameElements) {
                            StringBuilder fullName = new StringBuilder();
                            Element sibling = nameElement.nextElementSibling();
                            
                            while (sibling != null && sibling.tagName().equals("span")) {
                                fullName.append(sibling.text()).append(" ");
                                sibling = sibling.nextElementSibling();
                            }
                            String name = fullName.toString().trim(); 

                            Elements nameTypes = nameEntry.select("b:contains(Name Type:) + span");
                            String nameType = nameTypes.isEmpty() ? "" : nameTypes.first().text();

                            

                            if (nameType.equalsIgnoreCase("Primary Name")) {
                                primaryName = name;
                            } 
                        }
                    }

                    if (!primaryName.isEmpty() || aliases.length() > 0) {
                       /* System.out.printf("Writing entry: %s, %s, %s, %s, %s%n",
                            "UK", type, primaryName, aliases.toString(), country);*/ //for debugging
                        String allAliasesStr = aliases.toString().trim();
                        String[] allAliases = allAliasesStr.isEmpty() ? new String[]{"null"} : allAliasesStr.split(";");
                        for (String alias : allAliases) {
                            writer.write(String.format("%s,%s,%s,%s,%s",
                                escapeCsv("UK"), escapeCsv(type), escapeCsv(primaryName), escapeCsv(alias.trim()), escapeCsv(country)));
                            writer.newLine();
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static void writeToCsvIMO(Document document, String csvFilePath) {
        try {
            File csvFile = new File(csvFilePath);
            boolean fileExists = csvFile.exists();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, false))) { 
                if (!fileExists) {
                    writer.write("File,IMOnum,Name");
                    writer.newLine();
                }

                Elements divs = document.select("div");

                String imoNumber = null;
                String primaryName = null;
                boolean isShip = false;
                writer.write("File,IMOnum,Name");
                writer.newLine();

                for (Element div : divs) {
                    String divText = div.text().trim();

                    if (divText.contains("- Ship")) {
                        isShip = true;
                    }

                    if (isShip && divText.contains("IMO Numbers:")) {
                        Element imoElement = div.selectFirst("span");
                        if (imoElement != null) {
                            imoNumber = imoElement.text().trim();
                            System.out.println("Found IMO Number: " + imoNumber);
                        }
                    }

                    if (isShip && divText.contains("Name Type: Primary name")) {
                        Element nameElement = div.selectFirst("span");
                        if (nameElement != null) {
                            primaryName = nameElement.text().trim();
                            System.out.println("Found Primary Name: " + primaryName);
                        }
                    }

                    if (imoNumber != null && primaryName != null) {
                    	
                        writer.write(String.format("%s,%s,%s", escapeCsv("UK"), escapeCsv(imoNumber), escapeCsv(primaryName)));
                        writer.newLine();

                        imoNumber = null;
                        primaryName = null;
                        isShip = false;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
