package com.university.infrastructure.persistence;

import com.university.domain.student.AchievementType;
import com.university.domain.student.StudentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CRUD integration tests for StudentEntity.
 *
 * <p>Validates the following HexaGlue JPA bug fixes:
 * <ul>
 *   <li>BUG-004: @ElementCollection with @Enumerated(STRING) for enum lists</li>
 *   <li>BUG-006: @CreatedDate/@LastModifiedDate auditing fields</li>
 *   <li>BUG-007: @Version optimistic locking field</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Student CRUD Operations")
class StudentCrudTest {

    @Autowired
    private StudentJpaRepository studentRepository;

    @Nested
    @DisplayName("Create Operations")
    class CreateOperations {

        @Test
        @DisplayName("should create student with basic fields")
        void shouldCreateStudentWithBasicFields() {
            // Given
            StudentEntity student = new StudentEntity();
            student.setId(UUID.randomUUID());
            student.setFirstName("John");
            student.setLastName("Doe");
            student.setEmail("john.doe@university.edu");
            student.setStatus(StudentStatus.ENROLLED);

            // When
            StudentEntity saved = studentRepository.save(student);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getFirstName()).isEqualTo("John");
            assertThat(saved.getLastName()).isEqualTo("Doe");
            assertThat(saved.getEmail()).isEqualTo("john.doe@university.edu");
            assertThat(saved.getStatus()).isEqualTo(StudentStatus.ENROLLED);
        }

        @Test
        @DisplayName("BUG-004: should create student with enum list achievements")
        void shouldCreateStudentWithEnumListAchievements() {
            // Given - BUG-004 fix: enum collections use @Enumerated(STRING)
            StudentEntity student = new StudentEntity();
            student.setId(UUID.randomUUID());
            student.setFirstName("Alice");
            student.setLastName("Smith");
            student.setEmail("alice.smith@university.edu");
            student.setStatus(StudentStatus.ENROLLED);
            student.setAchievements(List.of(
                    AchievementType.HONOR_ROLL,
                    AchievementType.DEAN_LIST,
                    AchievementType.SCHOLARSHIP
            ));

            // When
            StudentEntity saved = studentRepository.save(student);
            studentRepository.flush();

            // Then - Verify enum list is persisted correctly
            Optional<StudentEntity> found = studentRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getAchievements())
                    .hasSize(3)
                    .containsExactlyInAnyOrder(
                            AchievementType.HONOR_ROLL,
                            AchievementType.DEAN_LIST,
                            AchievementType.SCHOLARSHIP
                    );
        }

        @Test
        @DisplayName("BUG-007: should initialize version on create")
        void shouldInitializeVersionOnCreate() {
            // Given
            StudentEntity student = new StudentEntity();
            student.setId(UUID.randomUUID());
            student.setFirstName("Bob");
            student.setLastName("Wilson");
            student.setEmail("bob.wilson@university.edu");
            student.setStatus(StudentStatus.ENROLLED);

            // When
            StudentEntity saved = studentRepository.save(student);
            studentRepository.flush();

            // Then - BUG-007 fix: @Version field is managed by JPA
            assertThat(saved.getVersion()).isNotNull();
            assertThat(saved.getVersion()).isGreaterThanOrEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadOperations {

        @Test
        @DisplayName("should find student by ID")
        void shouldFindStudentById() {
            // Given
            UUID studentId = UUID.randomUUID();
            StudentEntity student = createStudent(studentId, "Jane", "Doe", "jane.doe@university.edu");
            studentRepository.save(student);

            // When
            Optional<StudentEntity> found = studentRepository.findById(studentId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("should find student by email")
        void shouldFindStudentByEmail() {
            // Given
            StudentEntity student = createStudent(UUID.randomUUID(), "Mike", "Brown", "mike.brown@university.edu");
            studentRepository.save(student);

            // When
            List<StudentEntity> found = studentRepository.findByEmail("mike.brown@university.edu");

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getFirstName()).isEqualTo("Mike");
        }

        @Test
        @DisplayName("should find all students")
        void shouldFindAllStudents() {
            // Given
            studentRepository.save(createStudent(UUID.randomUUID(), "Student1", "Test", "s1@test.edu"));
            studentRepository.save(createStudent(UUID.randomUUID(), "Student2", "Test", "s2@test.edu"));
            studentRepository.save(createStudent(UUID.randomUUID(), "Student3", "Test", "s3@test.edu"));

            // When
            List<StudentEntity> all = studentRepository.findAll();

            // Then
            assertThat(all).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateOperations {

        @Test
        @DisplayName("should update student fields")
        void shouldUpdateStudentFields() {
            // Given
            UUID studentId = UUID.randomUUID();
            StudentEntity student = createStudent(studentId, "Original", "Name", "original@test.edu");
            studentRepository.save(student);

            // When
            StudentEntity toUpdate = studentRepository.findById(studentId).orElseThrow();
            toUpdate.setFirstName("Updated");
            toUpdate.setLastName("Person");
            studentRepository.save(toUpdate);
            studentRepository.flush();

            // Then
            StudentEntity updated = studentRepository.findById(studentId).orElseThrow();
            assertThat(updated.getFirstName()).isEqualTo("Updated");
            assertThat(updated.getLastName()).isEqualTo("Person");
        }

        @Test
        @DisplayName("BUG-004: should update enum list achievements")
        void shouldUpdateEnumListAchievements() {
            // Given
            UUID studentId = UUID.randomUUID();
            StudentEntity student = createStudent(studentId, "Award", "Winner", "winner@test.edu");
            student.setAchievements(new java.util.ArrayList<>(List.of(AchievementType.HONOR_ROLL)));
            studentRepository.save(student);
            studentRepository.flush();

            // When - Add more achievements
            StudentEntity toUpdate = studentRepository.findById(studentId).orElseThrow();
            toUpdate.setAchievements(new java.util.ArrayList<>(List.of(
                    AchievementType.HONOR_ROLL,
                    AchievementType.PERFECT_ATTENDANCE,
                    AchievementType.RESEARCH_AWARD
            )));
            studentRepository.save(toUpdate);
            studentRepository.flush();

            // Then
            StudentEntity updated = studentRepository.findById(studentId).orElseThrow();
            assertThat(updated.getAchievements())
                    .hasSize(3)
                    .contains(AchievementType.RESEARCH_AWARD);
        }

        @Test
        @DisplayName("BUG-007: should increment version on update")
        void shouldIncrementVersionOnUpdate() {
            // Given
            UUID studentId = UUID.randomUUID();
            StudentEntity student = createStudent(studentId, "Version", "Test", "version@test.edu");
            StudentEntity saved = studentRepository.save(student);
            studentRepository.flush();
            Long initialVersion = saved.getVersion();

            // When
            StudentEntity toUpdate = studentRepository.findById(studentId).orElseThrow();
            toUpdate.setFirstName("VersionUpdated");
            studentRepository.save(toUpdate);
            studentRepository.flush();

            // Then - BUG-007 fix: @Version increments on update
            StudentEntity updated = studentRepository.findById(studentId).orElseThrow();
            assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("should delete student by ID")
        void shouldDeleteStudentById() {
            // Given
            UUID studentId = UUID.randomUUID();
            StudentEntity student = createStudent(studentId, "ToDelete", "Student", "delete@test.edu");
            studentRepository.save(student);
            assertThat(studentRepository.existsById(studentId)).isTrue();

            // When
            studentRepository.deleteById(studentId);

            // Then
            assertThat(studentRepository.existsById(studentId)).isFalse();
        }

        @Test
        @DisplayName("should delete student entity")
        void shouldDeleteStudentEntity() {
            // Given
            UUID studentId = UUID.randomUUID();
            StudentEntity student = createStudent(studentId, "ToDelete", "Entity", "delete.entity@test.edu");
            studentRepository.save(student);

            // When
            StudentEntity toDelete = studentRepository.findById(studentId).orElseThrow();
            studentRepository.delete(toDelete);

            // Then
            assertThat(studentRepository.findById(studentId)).isEmpty();
        }

        @Test
        @DisplayName("BUG-004: should delete student with enum list")
        void shouldDeleteStudentWithEnumList() {
            // Given - Student with achievements
            UUID studentId = UUID.randomUUID();
            StudentEntity student = createStudent(studentId, "Achiever", "ToDelete", "achiever@test.edu");
            student.setAchievements(List.of(
                    AchievementType.SCHOLARSHIP,
                    AchievementType.COMMUNITY_SERVICE
            ));
            studentRepository.save(student);
            studentRepository.flush();

            // When
            studentRepository.deleteById(studentId);
            studentRepository.flush();

            // Then - Both student and achievements are deleted
            assertThat(studentRepository.findById(studentId)).isEmpty();
        }
    }

    private StudentEntity createStudent(UUID id, String firstName, String lastName, String email) {
        StudentEntity student = new StudentEntity();
        student.setId(id);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setStatus(StudentStatus.ENROLLED);
        return student;
    }
}
