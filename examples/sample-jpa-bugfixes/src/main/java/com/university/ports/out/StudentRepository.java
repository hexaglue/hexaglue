package com.university.ports.out;

import com.university.domain.student.Student;
import com.university.domain.student.StudentId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Student aggregate.
 *
 * <p>Generated adapter will demonstrate:
 * <ul>
 *   <li>BUG-004: @ElementCollection with @Enumerated for achievements list</li>
 *   <li>BUG-006: @CreatedDate/@LastModifiedDate fields</li>
 *   <li>BUG-007: @Version field for optimistic locking</li>
 * </ul>
 */
public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(StudentId id);

    List<Student> findAll();

    void delete(Student student);

    List<Student> findByEmail(String email);
}
