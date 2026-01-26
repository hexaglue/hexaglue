package com.university.ports.out;

import com.university.domain.course.Course;
import com.university.domain.course.CourseId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Course aggregate.
 *
 * <p>Generated adapter will demonstrate:
 * <ul>
 *   <li>BUG-001: @ManyToMany with @JoinTable for tags relationship</li>
 *   <li>BUG-002: @JoinColumn for lessons relationship (via Lesson entity)</li>
 *   <li>BUG-003: Bidirectional relationship with tags</li>
 *   <li>BUG-006: Auditing fields</li>
 *   <li>BUG-007: Optimistic locking</li>
 * </ul>
 */
public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(CourseId id);

    List<Course> findAll();

    void delete(Course course);

    List<Course> findByTitle(String title);

    List<Course> findByActiveTrue();
}
