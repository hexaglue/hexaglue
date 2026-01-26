package com.university;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * University Management Application.
 *
 * <p>This application demonstrates all JPA plugin bug fixes from the 2026-01-26 campaign:
 *
 * <h3>Bug Fixes Demonstrated</h3>
 * <ul>
 *   <li><b>BUG-001</b>: @ManyToMany generates @JoinTable (Course ↔ Tag)</li>
 *   <li><b>BUG-002</b>: @ManyToOne generates @JoinColumn (Lesson → Course)</li>
 *   <li><b>BUG-003</b>: Bidirectional relationships detected (Tag.courses uses mappedBy)</li>
 *   <li><b>BUG-004</b>: @ElementCollection of enums uses @Enumerated(STRING) (Student.achievements)</li>
 *   <li><b>BUG-006</b>: enableAuditing generates @CreatedDate/@LastModifiedDate fields</li>
 *   <li><b>BUG-007</b>: enableOptimisticLocking generates @Version field</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <p>See hexaglue.yaml for JPA plugin configuration with:
 * <ul>
 *   <li>enableAuditing: true</li>
 *   <li>enableOptimisticLocking: true</li>
 *   <li>tablePrefix: univ_</li>
 * </ul>
 *
 * @see com.university.domain.student.Student
 * @see com.university.domain.course.Course
 * @see com.university.domain.course.Tag
 */
@SpringBootApplication
@EnableJpaAuditing
public class UniversityApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniversityApplication.class, args);
    }
}
