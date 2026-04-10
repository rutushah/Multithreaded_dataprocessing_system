package com;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.*;

public class DataProcessingSystem {

    private static final int NUM_WORKERS = 4;
    private static final int NUM_TASKS = 20;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tH:%1$tM:%1$tS.%1$tL] [%4$s] %5$s%n");

        Logger rootLogger = Logger.getLogger("");

        // Remove all existing handlers (these print to stderr = red in IntelliJ)
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Add a new ConsoleHandler pointed at System.out (white in IntelliJ)
        ConsoleHandler consoleHandler = new ConsoleHandler() {
            {
                setOutputStream(System.out);
                setFormatter(new SimpleFormatter());
                setLevel(Level.INFO);
            }
        };

        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(Level.INFO);
    }

    private static final Logger LOGGER = Logger.getLogger(DataProcessingSystem.class.getName());

    public static void main(String[] args) {
        LOGGER.info("=== Data Processing System - Java ===");
        LOGGER.info(String.format("Config: %d workers, %d tasks", NUM_WORKERS, NUM_TASKS));

        SharedQueue taskQueue = new SharedQueue();
        ResultStore resultStore = new ResultStore();

        // 1. Populate the queue
        LOGGER.info("Populating task queue...");
        String[] dataTypes = {"SENSOR_READ", "LOG_PARSE", "IMAGE_RESIZE",
                "DB_QUERY", "API_CALL", "FILE_COMPRESS"};
        for (int i = 1; i <= NUM_TASKS; i++) {
            String data = dataTypes[i % dataTypes.length] + "_" + i;
            int priority = (i % 3) + 1;
            taskQueue.addTask(new Task(i, data, priority));
        }
        LOGGER.info("Queue populated with " + NUM_TASKS + " tasks.");

        // 2. Start workers via ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(NUM_WORKERS);
        List<Worker> workers = new ArrayList<>();
        for (int i = 1; i <= NUM_WORKERS; i++) {
            Worker worker = new Worker("Worker-" + i, taskQueue, resultStore);
            workers.add(worker);
            executor.submit(worker);
        }

        // 3. Signal shutdown after all tasks submitted
        taskQueue.shutdown();

        // 4. Wait for workers to finish
        executor.shutdown();
        try {
            boolean finished = executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                LOGGER.warning("Timeout reached. Forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.severe("Main thread interrupted: " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 5. Print summary
        LOGGER.info("=== RESULTS SUMMARY ===");
        LOGGER.info("Total results stored: " + resultStore.size());
        for (Worker w : workers) {
            LOGGER.info(String.format("  %s -> completed=%d, errors=%d",
                    w.getName(), w.getTasksCompleted(), w.getErrorsEncountered()));
        }
        resultStore.getResults().stream().limit(5)
                .forEach(r -> LOGGER.info("  " + r.toString()));

        // 6. Write to file
        try {
            resultStore.writeToFile("results.txt");
        } catch (IOException e) {
            LOGGER.severe("Could not write results file: " + e.getMessage());
        }

        LOGGER.info("System shutdown complete.");
    }
}