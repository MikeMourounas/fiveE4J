package com.fiveE4J.textgen.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BasicCrawler {
    private static final Logger logger = LoggerFactory.getLogger(BasicCrawler.class);

    /**
     * List of processed URLs
     */
    private List<String> processed;

    public BasicCrawler() {
        this.processed = new ArrayList<>();
    }

    // Methods

    /**
     * Check if crawler has already seen the given URL
     *
     * @param URL
     * @return whether URL has been already by processed
     */
    protected boolean isProcessed(String URL) {
        return processed.contains(URL);
    }

    /**
     * Add new URL to list of processed pages
     * @param URL
     */
    protected void addProcessedURL(String URL) {
        this.processed.add(URL);
    }

    /**
     * Recursive method; scrape through current URL for desired content
     *
     * @param URL
     * @throws IOException
     */
    public abstract void processPage(String URL) throws IOException;

    /**
     * Write data to desired output path
     *
     * @param data
     * @param outputPath
     */
    public abstract void writeData(Collection data, String outputPath) throws IOException;
}
