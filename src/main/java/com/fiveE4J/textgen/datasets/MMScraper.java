package com.fiveE4J.textgen.datasets;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick crawler to scrape all monster entries from the 5e srd
 *
 * @author mike
 * todo: generalize the scraping to an abstract class
 * todo: create scrapers for each srd category (eg., items, spells, etc.)
 */
public class MMScraper {
    private static final Logger logger = LoggerFactory.getLogger(MMScraper.class);

    /**
     * List of web pages already scraped
     */
    private static List<String> processesPages = new ArrayList<>();

    /**
     * Hash set of monster manual entries from the srd
     */
    private static Set<String> entries = new HashSet<>();

    /**
     * Has this page been processed already?
     *
     * @param page
     * @return
     */
    private static boolean isProcessed(String page) {
        if (processesPages.contains(page))
            return true;
        else
            return false;
    }

    /**
     * Main method for processing pages of html from the 5e srd. Specifically
     * looking for monster entries at the moment, but should probably be
     * generalized.
     *
     * @param URL
     * @throws IOException
     */
    private static void processPage(String URL) throws IOException {
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
            processesPages.add(URL);

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
            entries.add(entry.toString());

            Elements questions = doc.select("a[href]");
            for (Element link : questions)
                processPage(link.attr("abs:href"));

            logger.info("Total entries: " + entries.size());
        } else
            return;
    }

    /**
     * Write entries to text file
     *
     * @throws IOException
     */
    private static void writeEntries() throws IOException {
        File file = new File("src/main/resources/mm_srd_data.txt");
        FileOutputStream os = new FileOutputStream(file);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

        logger.info("Writing entries to data file...");

        for (String entry : entries) {
            writer.write(entry);
            writer.newLine();
        }
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        String URL = "https://www.5esrd.com/gamemastering/monsters-foes/monsters-by-type/aberrations/aboleth/";
        processPage(URL);
        writeEntries();
    }
}
