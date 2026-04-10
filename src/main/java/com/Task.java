package com;

public class Task {
    private final int id;
    private final String data;
    private final int priority;

    public Task(int id, String data, int priority) {
        this.id = id;
        this.data = data;
        this.priority = priority;
    }

    public int getId() { return id; }
    public String getData() { return data; }
    public int getPriority() { return priority; }

    @Override
    public String toString() {
        return String.format("Task[id=%d, priority=%d, data='%s']", id, priority, data);
    }
}