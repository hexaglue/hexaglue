package com.example.ports.in;

import com.example.domain.Task;
import com.example.domain.TaskId;
import java.util.List;
import java.util.Optional;

/**
 * Primary port for task management use cases.
 *
 * <p>This is a driving port that defines the operations
 * available to external actors (UI, API, etc.).
 */
public interface TaskUseCases {

    /**
     * Creates a new task.
     *
     * @param title the task title
     * @param description the task description
     * @return the created task
     */
    Task createTask(String title, String description);

    /**
     * Retrieves a task by its ID.
     *
     * @param id the task identifier
     * @return the task if found
     */
    Optional<Task> getTask(TaskId id);

    /**
     * Lists all tasks.
     *
     * @return all tasks
     */
    List<Task> listAllTasks();

    /**
     * Marks a task as completed.
     *
     * @param id the task identifier
     */
    void completeTask(TaskId id);

    /**
     * Deletes a task.
     *
     * @param id the task identifier
     */
    void deleteTask(TaskId id);
}
