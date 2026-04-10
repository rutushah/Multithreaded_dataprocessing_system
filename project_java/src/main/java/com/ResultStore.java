package com;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class ResultStore {
    private static final Logger LOGGER = Logger.getLogger(ResultStore.class.getName());

    private final List<TaskResult> results = new ArrayList<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    public void addResult(TaskResult result) {
        writeLock.lock();
        try {
            results.add(result);
        } finally {
            writeLock.unlock();
        }
    }

    public List<TaskResult> getResults() {
        readLock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(results));
        } finally {
            readLock.unlock();
        }
    }

    public int size() {
        readLock.lock();
        try {
            return results.size();
        } finally {
            readLock.unlock();
        }
    }

    public void writeToFile(String filename) throws IOException {
        readLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("=== Data Processing System Results ===\n");
            writer.write("Total results: " + results.size() + "\n\n");
            for (TaskResult result : results) {
                writer.write(result.toString());
                writer.newLine();
            }
            LOGGER.info("Results written to: " + filename);
        } catch (IOException e) {
            LOGGER.severe("Failed to write results to file: " + e.getMessage());
            throw e;
        } finally {
            readLock.unlock();
        }
    }
}