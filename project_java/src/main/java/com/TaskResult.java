package com;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TaskResult {
    private final int taskId;
    private final String workerName;
    private final String result;
    private final long processingTimeMs;
    private final LocalDateTime completedAt;

    public TaskResult(int taskId, String workerName, String result, long processingTimeMs) {
        this.taskId = taskId;
        this.workerName = workerName;
        this.result = result;
        this.processingTimeMs = processingTimeMs;
        this.completedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("[%s] Worker=%-12s | TaskID=%3d | Time=%4dms | Result=%s",
                completedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                workerName, taskId, processingTimeMs, result);
    }
}