package com;

import java.util.Random;
import java.util.logging.Logger;

public class Worker implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());
    private static final int MIN_PROCESS_MS = 100;
    private static final int MAX_PROCESS_MS = 500;

    private final String name;
    private final SharedQueue taskQueue;
    private final ResultStore resultStore;
    private final Random random = new Random();

    private int tasksCompleted = 0;
    private int errorsEncountered = 0;

    public Worker(String name, SharedQueue taskQueue, ResultStore resultStore) {
        this.name = name;
        this.taskQueue = taskQueue;
        this.resultStore = resultStore;
    }

    @Override
    public void run() {
        LOGGER.info(name + " started.");

        while (true) {
            Task task = null;

            try {
                task = taskQueue.getTask();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning(name + " interrupted while waiting for a task. Exiting.");
                break;
            }

            if (task == null) {
                LOGGER.info(name + " received shutdown signal. Exiting cleanly.");
                break;
            }

            try {
                TaskResult result = processTask(task);
                resultStore.addResult(result);
                tasksCompleted++;
                LOGGER.info(name + " completed " + task);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning(name + " interrupted during processing. Exiting.");
                errorsEncountered++;
                break;

            } catch (Exception e) {
                errorsEncountered++;
                LOGGER.severe(name + " error on " + task + ": " + e.getMessage());
            }
        }

        LOGGER.info(String.format("%s finished. Completed: %d, Errors: %d",
                name, tasksCompleted, errorsEncountered));
    }

    private TaskResult processTask(Task task) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        int delayMs = MIN_PROCESS_MS + random.nextInt(MAX_PROCESS_MS - MIN_PROCESS_MS)
                + (task.getPriority() * 20);
        Thread.sleep(delayMs);
        String resultData = String.format("PROCESSED[%s] hash=%d",
                task.getData().toUpperCase(),
                task.getData().hashCode() ^ task.getId());
        long elapsed = System.currentTimeMillis() - startTime;
        return new TaskResult(task.getId(), name, resultData, elapsed);
    }

    public String getName() { return name; }
    public int getTasksCompleted() { return tasksCompleted; }
    public int getErrorsEncountered() { return errorsEncountered; }
}