package com.fiveE4J.textgen.trainer;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.training.dataset.ArrayDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is nothing yet; just playing around with djl
 */
public class TestTrainer {
    private static final Logger logger = LoggerFactory.getLogger(TestTrainer.class);

    /**
     * Path to text data file
     */
    private static String datasetPath = "src/main/resources/mm_srd_data.txt";

    /**
     * Everything we'll need to build an ArrayDataset object of characters
     */
    private static NDArray array;
    private static NDManager manager;
    private static ArrayDataset.Builder builder;
    private static ArrayDataset dataset;

    public static void main(String[] args) {
        manager = NDManager.newBaseManager();
        array = manager.create(64);
        builder = new ArrayDataset.Builder();
        builder.setData(array);
        dataset = builder.build();
    }
}
