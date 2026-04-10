package com;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class SharedQueue {
    private static final Logger LOGGER = Logger.getLogger(SharedQueue.class.getName());

    private final Queue<Task> queue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notEmpty = lock.newCondition();

    private volatile boolean shutdown = false;
    private int totalAdded = 0;

    public void addTask(Task task) {
        lock.lock();
        try {
            if (shutdown) {
                LOGGER.warning("Queue is shut down. Task " + task.getId() + " rejected.");
                return;
            }
            queue.offer(task);
            totalAdded++;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public Task getTask() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (queue.isEmpty() && !shutdown) {
                notEmpty.await();
            }
            if (queue.isEmpty()) {
                return null;
            }
            return queue.poll();
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            shutdown = true;
            notEmpty.signalAll();
            LOGGER.info("SharedQueue shutting down. Total tasks added: " + totalAdded);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    public boolean isShutdown() { return shutdown; }
    public int getTotalAdded() { return totalAdded; }
}