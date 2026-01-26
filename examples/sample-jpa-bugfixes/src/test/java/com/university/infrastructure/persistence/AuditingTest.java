package com.university.infrastructure.persistence;

import com.university.domain.student.StudentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JPA Auditing functionality.
 *
 * <p>Validates HexaGlue JPA BUG-006 fix:
 * <ul>
 *   <li>@CreatedDate field is populated on entity creation</li>
 *   <li>@LastModifiedDate field is populated on creation and updated on modification</li>
 *   <li>@EntityListeners(AuditingEntityListener.class) is correctly configured</li>
 * </ul>
 *
 * <p>Uses @SpringBootTest to enable full Spring context with @EnableJpaAuditing.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("BUG-006: JPA Auditing")
class AuditingTest {

    @Autowired
    private StudentJpaRepository studentRepository;

    @Autowired
    private CourseJpaRepository courseRepository;

    @Autowired
    private TagJpaRepository tagRepository;

    @Test
    @DisplayName("should populate createdAt on entity creation")
    void shouldPopulateCreatedAtOnEntityCreation() {
        // Given
        Instant beforeCreate = Instant.now();

        StudentEntity student = new StudentEntity();
        student.setId(UUID.randomUUID());
        student.setFirstName("Audit");
        student.setLastName("Test");
        student.setEmail("audit.test@university.edu");
        student.setStatus(StudentStatus.ENROLLED);

        // When
        StudentEntity saved = studentRepository.saveAndFlush(student);

        Instant afterCreate = Instant.now();

        // Then - BUG-006: @CreatedDate is populated
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfterOrEqualTo(beforeCreate);
        assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(afterCreate);
    }

    @Test
    @DisplayName("should populate updatedAt on entity creation")
    void shouldPopulateUpdatedAtOnEntityCreation() {
        // Given
        Instant beforeCreate = Instant.now();

        StudentEntity student = new StudentEntity();
        student.setId(UUID.randomUUID());
        student.setFirstName("Audit");
        student.setLastName("UpdatedAt");
        student.setEmail("audit.updatedat@university.edu");
        student.setStatus(StudentStatus.ENROLLED);

        // When
        StudentEntity saved = studentRepository.saveAndFlush(student);

        Instant afterCreate = Instant.now();

        // Then - BUG-006: @LastModifiedDate is populated on creation
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(beforeCreate);
        assertThat(saved.getUpdatedAt()).isBeforeOrEqualTo(afterCreate);
    }

    @Test
    @DisplayName("should update updatedAt on entity modification")
    void shouldUpdateUpdatedAtOnEntityModification() throws InterruptedException {
        // Given - Create entity
        StudentEntity student = new StudentEntity();
        student.setId(UUID.randomUUID());
        student.setFirstName("Original");
        student.setLastName("Name");
        student.setEmail("original@university.edu");
        student.setStatus(StudentStatus.ENROLLED);

        StudentEntity saved = studentRepository.saveAndFlush(student);
        Instant createdAt = saved.getCreatedAt();
        Instant originalUpdatedAt = saved.getUpdatedAt();

        // Wait a bit to ensure timestamp difference
        Thread.sleep(50);

        // When - Modify entity
        saved.setFirstName("Modified");
        StudentEntity modified = studentRepository.saveAndFlush(saved);

        // Then - BUG-006: updatedAt is changed, createdAt remains the same
        assertThat(modified.getCreatedAt()).isEqualTo(createdAt);
        assertThat(modified.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("should not change createdAt on entity modification")
    void shouldNotChangeCreatedAtOnEntityModification() throws InterruptedException {
        // Given - Create entity
        StudentEntity student = new StudentEntity();
        student.setId(UUID.randomUUID());
        student.setFirstName("Immutable");
        student.setLastName("CreatedAt");
        student.setEmail("immutable.createdat@university.edu");
        student.setStatus(StudentStatus.ENROLLED);

        StudentEntity saved = studentRepository.saveAndFlush(student);
        Instant originalCreatedAt = saved.getCreatedAt();

        Thread.sleep(50);

        // When - Modify entity multiple times
        saved.setFirstName("Modified1");
        StudentEntity modified1 = studentRepository.saveAndFlush(saved);

        Thread.sleep(50);

        modified1.setFirstName("Modified2");
        StudentEntity modified2 = studentRepository.saveAndFlush(modified1);

        // Then - createdAt never changes
        assertThat(modified1.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(modified2.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    @DisplayName("should apply auditing to CourseEntity")
    void shouldApplyAuditingToCourseEntity() {
        // Given
        CourseEntity course = new CourseEntity();
        course.setId(UUID.randomUUID());
        course.setTitle("Audited Course");
        course.setDescription("Testing auditing");
        course.setCredits(3);
        course.setActive(true);

        // When
        CourseEntity saved = courseRepository.saveAndFlush(course);

        // Then - BUG-006: All entities have auditing
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should apply auditing to TagEntity")
    void shouldApplyAuditingToTagEntity() {
        // Given
        TagEntity tag = new TagEntity();
        tag.setId(UUID.randomUUID());
        tag.setName("Audited Tag");
        tag.setDescription("Testing auditing on tags");

        // When
        TagEntity saved = tagRepository.saveAndFlush(tag);

        // Then - BUG-006: All entities have auditing
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("createdAt and updatedAt should be equal on initial save")
    void createdAtAndUpdatedAtShouldBeEqualOnInitialSave() {
        // Given
        StudentEntity student = new StudentEntity();
        student.setId(UUID.randomUUID());
        student.setFirstName("Equal");
        student.setLastName("Timestamps");
        student.setEmail("equal.timestamps@university.edu");
        student.setStatus(StudentStatus.ENROLLED);

        // When
        StudentEntity saved = studentRepository.saveAndFlush(student);

        // Then - On creation, both timestamps should be very close (within same transaction)
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        // They might not be exactly equal due to clock precision, but should be within milliseconds
        long diff = Math.abs(saved.getCreatedAt().toEpochMilli() - saved.getUpdatedAt().toEpochMilli());
        assertThat(diff).isLessThan(100); // Less than 100ms difference
    }
}
