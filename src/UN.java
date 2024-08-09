import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UN {

    public static void main(String[] args) {
        String urlString = "https://scsanctions.un.org/consolidated";
        String csvFilePath = "parsed_UN.csv";
        String csvFileIMOUN = "IMO_UN.csv";

        try {
            Document document = Jsoup.connect(urlString).get();
            writeToCsvIMO(document,csvFileIMOUN);
            writeToCsv(document, csvFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeToCsv(Document document, String csvFilePath) {
        try {
            File csvFile = new File(csvFilePath);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, StandardCharsets.UTF_8, false))) {
              
                writer.write("File,Type,Name,Alias,Country");
                writer.newLine();

                Elements entries = document.select("table#sanctions tbody tr");

                for (Element entry : entries) {
                    String nameType = entry.select("span:contains(Individuals)").size() > 0 ? "Individual" : "Entity";
                    StringBuilder primaryName = new StringBuilder();
                    StringBuilder allAliases = new StringBuilder();
                    String country = "";

                    Elements tdElements = entry.select("td");
                    for (Element td : tdElements) {
                        String text = td.text();

                        if (text.contains("Name:")) {
                            String nameSection = text.split("Name:")[1].split("na|Passport no:|Nationality:|National identification no:|Designation:|Address:|Name \\(original script\\):| Title:")[0].trim();
                            String[] nameParts = nameSection.split("\\d+:");

                            for (String part : nameParts) {
                                String cleanedPart = removeParentheses(part.trim());
                                if (!cleanedPart.isEmpty()) {
                                    primaryName.append(cleanedPart).append(" ");
                                }
                            }
                            primaryName = new StringBuilder(primaryName.toString().trim());
                        }

                        if (text.contains("Good quality a.k.a.:") || text.contains("Low quality a.k.a.:")) {
                            String[] sections = text.split("Good quality a\\.k\\.a\\.:|Low quality a\\.k\\.a\\.:");
                            if (sections.length > 1) {
                                String aliasSection = sections[1].split("Nationality:|Passport no:|National identification no:|Address:|Name \\(original script\\):| born|Other information:")[0].trim();
                                String[] akaParts = aliasSection.split("\\d+:");
                                for (String akaPart : akaParts) {
                                    if (akaPart.trim().startsWith("a)")) {
                                        String alias = akaPart.substring(2).trim();
                                        alias = removeParentheses(alias);
                                        alias = alias.replaceAll(",", "").trim();
                                        alias = alias.replaceAll("Previously listed as\\s*|\\p{InArabic}", "").trim();
                                        alias = alias.replaceAll("\\b[a-zA-Z]\\)\\s*", ";").trim();
                                        alias = normalizeSpecialChars(alias);
                                        if (!alias.matches(".*\\b(false identity|fraudulent|related to|linked to|confiscated|Other information:|Passport number:Passport number)\\b.*")) {
                                            allAliases.append(alias).append("; ");
                                        }
                                    }
                                }
                            }
                        }

                        if (text.contains("Nationality:")) {
                            country = text.split("Nationality:")[1].split("Passport no:|National identification no:|POB:|DOB:|Address:|Name \\(original script\\):")[0].trim();
                            country = country.replaceAll("\\b[a-zA-Z]\\)\\s*", ";").trim();
                        }
                    }

                    String primaryNameStr = primaryName.toString().trim();
                    String allAliasesStr = allAliases.toString().trim();

                    if (!primaryNameStr.isEmpty()) {
                        if (allAliasesStr.isEmpty()) {
                            writer.write(String.format("%s,%s,%s,%s,%s",
                                escapeCsv("UN"), escapeCsv(nameType), escapeCsv(primaryNameStr), "null", escapeCsv(country)));
                            writer.newLine();
                        } else {
                            String[] aliases = allAliasesStr.split(";");
                            for (String alias : aliases) {
                                writer.write(String.format("%s,%s,%s,%s,%s",
                                    escapeCsv("UN"), escapeCsv(nameType), escapeCsv(primaryNameStr), escapeCsv(alias), escapeCsv(country)));
                                writer.newLine();
                            }
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

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, StandardCharsets.UTF_8, false))) {
               
                writer.write("File,IMOnum,Name");
                writer.newLine();

                Elements entries = document.select("tr.rowtext");

                for (Element entry : entries) {
                    String imoNumber = "";
                    String primaryName = "";

                    Element td = entry.selectFirst("td");
                    if (td != null) {
                        String text = td.text();

                        if (text.contains("IMO number:")) {
                            imoNumber = text.split("IMO number:")[1].split("\\.")[0].trim(); }

                        if (text.contains("Name:")) {
                            primaryName = text.split("Name:")[1].split("A.k.a.:|F.k.a.:|Address:|Listed on:|Other information:")[0].trim();
                        }
                    }

                    if (!imoNumber.isEmpty() && !primaryName.isEmpty()) {
                        writer.write(String.format("%s,%s,%s",
                            escapeCsv("UN"), escapeCsv(imoNumber), escapeCsv(primaryName)));
                        writer.newLine();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private static String normalizeSpecialChars(String value) {
        if (value == null) return "";
        
        value = value.replace("â€™", "’")
                     .replace("â€œ", "“")
                     .replace("â€�", "”")
                     .replace("â€“", "–")
                     .replace("â€¦", "…")
                     .replace("€™", "…")
                     .replace("€™ ", "…")
                     .replace("€˜", "…")
                     .replace("â€™", "’");
       
        return value;
    }

    private static String removeParentheses(String value) {
        if (value == null) return "";
        return value.replaceAll("\\(.*?\\)", "").trim();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "").replace(",", "");
    }
}
