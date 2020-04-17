package com.fiveE4J.textgen.datasets;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick crawler to scrape all monster entries from the 5e srd
 *
 * @author mike
 * todo: create scrapers for each srd category (eg., items, spells, etc.)
 */
public class MMScraper extends BasicCrawler {
    private static final Logger logger = LoggerFactory.getLogger(MMScraper.class);

    private static final String URL = "https://www.5esrd.com/gamemastering/monsters-foes/monsters-by-type/aberrations/aboleth/";

    private static final String OUTPUT_PATH = "src/main/resources/mm_srd_data.txt";

    /**
     * Set of monster manual entries
     */
    private Set<String> data;

    // Constructor

    public MMScraper() {
        super();
        this.data = new HashSet<>();
    }

    // Getters & Setters

    public Set<String> getData() { return this.data; }

    // Methods

    /**
     * Add entry to data field
     *
     * @param entry
     */
    protected void addEntry(String entry) {
        this.data.add(entry);
    }

    /**
     * Implement abstract writeData method from BasicCrawler
     *
     * @param data
     * @param outputPath
     * @throws IOException
     */
    @Override
    public void writeData(Collection data, String outputPath) throws IOException {
        File file = new File(outputPath);
        FileOutputStream os = new FileOutputStream(file);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

        logger.info("Writing monster entries to output file ...");

        for (var entry : data) {
            writer.write((String) entry);
            writer.newLine();
        }
        writer.close();
    }

    /**
     * Main method for processing pages of html from the 5e srd. Specifically
     * looking for monster entries at the moment, but should probably be
     * generalized.
     *
     * @param URL
     * @throws IOException
     */
    @Override
    public void processPage(String URL) throws IOException {
        // Weird stuff encountered, go back...
        if (URL.contains(".pdf") || URL.contains("@")
                || URL.contains("adfad") || URL.contains(":80")
                || URL.contains("fdafd") || URL.contains(".jpg")
                || URL.contains(".pdf") || URL.contains(".jpg"))
            return;

        // We've exited the monster entries directory, go back
        if (!URL.contains("www.5esrd.com/gamemastering/monsters-foes/monsters-by-type/"))
            return;

        if (!isProcessed(URL)) {
            // Add current URL to list of processed pages
            addProcessedURL(URL);

            // Attempt to connect to the site via jsoup
            Document doc;
            try {
                doc = Jsoup.connect(URL).get();
            } catch (HttpStatusException e) {
                logger.info(e.toString());
                return;
            }

            // Extract the title
            Elements title = doc.getElementsByTag("title");

            // Initialize entry, getting rid of boilerplate
            StringBuilder entry = new StringBuilder(title.text().replaceAll(" – 5th Edition SRD", "") + "\n\n");

            // Prepare list of elements to be traversed
            Elements div = doc.getElementsByClass("article-content");

            // We don't need to process non-entry pages, but we still want their links.
            boolean nonEntry = false;
            if (!div.text().contains("Hit Points"))
                nonEntry = true;

            // Iterate through all elements in div (only one for these entries)
            for (Element e : div) {
                if (nonEntry)
                    break;

                // Get all children of current element
                Elements children = e.children();

                // Iterate through all children
                for (Element child : children) {
                    String text = "";
                    if (!child.hasText())
                        continue;
                    else if (child.text().contains("Variants "))
                        text = child.text().replace("Variants", "Variants:");
                    // Reformat ability scores to be more readable.
                    else if (child.text().startsWith("STR")) {
                        Pattern p = Pattern.compile("STR\\sDEX\\sCON\\sINT\\sWIS\\sCHA\\s" +
                                "(\\d+\\s?\\([+\\-–‒]?\\d+\\))\\s" +
                                "(\\d+\\s?\\([+\\-–‒]?\\d+\\))\\s" +
                                "(\\d+\\s?\\([+\\-–‒]?\\d+\\))\\s" +
                                "(\\d+\\s?\\([+\\-–‒]?\\d+\\))\\s" +
                                "(\\d+\\s?\\([+\\-–‒]?\\d+\\))\\s" +
                                "(\\d+\\s?\\([+\\-–‒]?\\d+\\))");
                        Matcher m = p.matcher(child.text());
                        if (m.find()) {
                            text = "STR " + m.group(1) + "\n" +
                                    "DEX " + m.group(2) + "\n" +
                                    "CON " + m.group(3) + "\n" +
                                    "INT " + m.group(4) + "\n" +
                                    "WIS " + m.group(5) + "\n" +
                                    "CHA " + m.group(6);
                        } else
                            text = child.text();
                    } else if (child.is("ul")) { // Reformat lists, like actions
                        for (int i = 0; i < child.childrenSize(); i++) {
                            Element li = child.child(i);
                            if (i == child.childNodeSize() - 1) {
                                text += li.text();
                            } else
                                text += li.text() + "\n";
                        }
                    } else if (child.text().contains("Copyright Notice")) {
                        // leave out copyright
                    } else
                        text = child.text();

                    if (!text.isEmpty())
                        entry.append(text).append("\n\n");
                }
            }
            addEntry(entry.toString());

            Elements questions = doc.select("a[href]");
            for (Element link : questions)
                processPage(link.attr("abs:href"));

            logger.info("Total entries: " + getData().size());
        } else
            return;
    }

    public static void main(String[] args) throws IOException {
        // Simple test script
        MMScraper scraper = new MMScraper();

        // scrape the srd
        scraper.processPage(URL);

        // write to file
        scraper.writeData(scraper.getData(), OUTPUT_PATH);
    }
}
