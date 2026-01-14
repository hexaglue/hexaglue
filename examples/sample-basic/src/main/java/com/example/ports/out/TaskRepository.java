package com.example.ports.out;

import com.example.domain.Task;
import com.example.domain.TaskId;
import java.util.List;
import java.util.Optional;

/**
 * Secondary port for task persistence.
 *
 * <p>This is a driven port that defines the operations
 * the domain needs from the persistence layer.
 */
public interface TaskRepository {

    /**
     * Saves a task.
     *
     * @param task the task to save
     * @return the saved task
     */
    Task save(Task task);

    /**
     * Finds a task by its ID.
     *
     * @param id the task identifier
     * @return the task if found
     */
    Optional<Task> findById(TaskId id);

    /**
     * Retrieves all tasks.
     *
     * @return all tasks
     */
    List<Task> findAll();

    /**
     * Deletes a task.
     *
     * @param task the task to delete
     */
    void delete(Task task);
}
