package com.example.domain;

import java.time.Instant;

/**
 * Task aggregate root.
 *
 * <p>Represents a task in a simple task management system.
 * This is the main domain entity managed by the application.
 */
public class Task {

    private final TaskId id;
    private String title;
    private String description;
    private boolean completed;
    private final Instant createdAt;

    /**
     * Creates a new task.
     *
     * @param id the task identifier
     * @param title the task title
     * @param description the task description
     */
    public Task(TaskId id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = false;
        this.createdAt = Instant.now();
    }

    public TaskId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Updates the task details.
     */
    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    /**
     * Marks the task as completed.
     */
    public void complete() {
        this.completed = true;
    }

    /**
     * Reopens a completed task.
     */
    public void reopen() {
        this.completed = false;
    }
}
