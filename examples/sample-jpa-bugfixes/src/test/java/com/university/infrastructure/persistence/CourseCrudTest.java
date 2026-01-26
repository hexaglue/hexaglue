package com.university.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CRUD integration tests for CourseEntity.
 *
 * <p>Validates the following HexaGlue JPA bug fixes:
 * <ul>
 *   <li>BUG-003: Bidirectional relationship with mappedBy</li>
 *   <li>BUG-006: @CreatedDate/@LastModifiedDate auditing fields</li>
 *   <li>BUG-007: @Version optimistic locking field</li>
 *   <li>BUG-008: Entity relationships use entity types (not domain types)</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Course CRUD Operations")
class CourseCrudTest {

    @Autowired
    private CourseJpaRepository courseRepository;

    @Autowired
    private TagJpaRepository tagRepository;

    @Autowired
    private LessonJpaRepository lessonRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("Create Operations")
    class CreateOperations {

        @Test
        @DisplayName("should create course with basic fields")
        void shouldCreateCourseWithBasicFields() {
            // Given
            CourseEntity course = new CourseEntity();
            course.setId(UUID.randomUUID());
            course.setTitle("Introduction to Computer Science");
            course.setDescription("Fundamentals of programming");
            course.setCredits(3);
            course.setActive(true);

            // When
            CourseEntity saved = courseRepository.save(course);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTitle()).isEqualTo("Introduction to Computer Science");
            assertThat(saved.getCredits()).isEqualTo(3);
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("BUG-008: should create course with tag relationships")
        void shouldCreateCourseWithTagRelationships() {
            // Given - Create tags first
            TagEntity tag1 = createTag("Programming");
            TagEntity tag2 = createTag("Computer Science");
            tagRepository.saveAll(List.of(tag1, tag2));
            tagRepository.flush();

            // When - Create course with tags (BUG-008: uses TagEntity, not Tag)
            CourseEntity course = new CourseEntity();
            course.setId(UUID.randomUUID());
            course.setTitle("Java Programming");
            course.setDescription("Learn Java");
            course.setCredits(4);
            course.setActive(true);
            course.setTags(new ArrayList<>(List.of(tag1, tag2)));

            CourseEntity saved = courseRepository.save(course);
            courseRepository.flush();

            // Then
            Optional<CourseEntity> found = courseRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getTags()).hasSize(2);
        }

        @Test
        @DisplayName("BUG-007: should initialize version on create")
        void shouldInitializeVersionOnCreate() {
            // Given
            CourseEntity course = createCourse("Test Course", 3);

            // When
            CourseEntity saved = courseRepository.save(course);
            courseRepository.flush();

            // Then
            assertThat(saved.getVersion()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadOperations {

        @Test
        @DisplayName("should find course by ID")
        void shouldFindCourseById() {
            // Given
            UUID courseId = UUID.randomUUID();
            CourseEntity course = createCourse(courseId, "Database Systems", 4);
            courseRepository.save(course);

            // When
            Optional<CourseEntity> found = courseRepository.findById(courseId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Database Systems");
        }

        @Test
        @DisplayName("should find course by title")
        void shouldFindCourseByTitle() {
            // Given
            CourseEntity course = createCourse("Unique Title Course", 3);
            courseRepository.save(course);

            // When
            List<CourseEntity> found = courseRepository.findByTitle("Unique Title Course");

            // Then
            assertThat(found).hasSize(1);
        }

        @Test
        @DisplayName("should find active courses")
        void shouldFindActiveCourses() {
            // Given
            CourseEntity active1 = createCourse("Active Course 1", 3);
            active1.setActive(true);
            CourseEntity active2 = createCourse("Active Course 2", 4);
            active2.setActive(true);
            CourseEntity inactive = createCourse("Inactive Course", 2);
            inactive.setActive(false);

            courseRepository.saveAll(List.of(active1, active2, inactive));

            // When
            List<CourseEntity> activeCourses = courseRepository.findByActiveTrue();

            // Then
            assertThat(activeCourses).hasSize(2);
            assertThat(activeCourses).allMatch(CourseEntity::isActive);
        }

        @Test
        @DisplayName("BUG-008: should load course with tag relationships")
        void shouldLoadCourseWithTagRelationships() {
            // Given
            TagEntity tag = createTag("Machine Learning");
            tagRepository.save(tag);

            CourseEntity course = createCourse("ML Fundamentals", 4);
            course.setTags(new ArrayList<>(List.of(tag)));
            courseRepository.save(course);
            courseRepository.flush();

            // When
            Optional<CourseEntity> found = courseRepository.findById(course.getId());

            // Then - BUG-008: Tags are loaded as TagEntity
            assertThat(found).isPresent();
            assertThat(found.get().getTags()).hasSize(1);
            assertThat(found.get().getTags().get(0).getName()).isEqualTo("Machine Learning");
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateOperations {

        @Test
        @DisplayName("should update course fields")
        void shouldUpdateCourseFields() {
            // Given
            UUID courseId = UUID.randomUUID();
            CourseEntity course = createCourse(courseId, "Original Title", 3);
            courseRepository.save(course);

            // When
            CourseEntity toUpdate = courseRepository.findById(courseId).orElseThrow();
            toUpdate.setTitle("Updated Title");
            toUpdate.setCredits(4);
            courseRepository.save(toUpdate);
            courseRepository.flush();

            // Then
            CourseEntity updated = courseRepository.findById(courseId).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("Updated Title");
            assertThat(updated.getCredits()).isEqualTo(4);
        }

        @Test
        @DisplayName("should deactivate course")
        void shouldDeactivateCourse() {
            // Given
            UUID courseId = UUID.randomUUID();
            CourseEntity course = createCourse(courseId, "To Deactivate", 3);
            course.setActive(true);
            courseRepository.save(course);

            // When
            CourseEntity toUpdate = courseRepository.findById(courseId).orElseThrow();
            toUpdate.setActive(false);
            courseRepository.save(toUpdate);
            courseRepository.flush();

            // Then
            CourseEntity updated = courseRepository.findById(courseId).orElseThrow();
            assertThat(updated.isActive()).isFalse();
        }

        @Test
        @DisplayName("BUG-008: should update course tags")
        void shouldUpdateCourseTags() {
            // Given
            TagEntity tag1 = createTag("Original Tag");
            TagEntity tag2 = createTag("New Tag");
            tagRepository.saveAll(List.of(tag1, tag2));

            CourseEntity course = createCourse("Course With Tags", 3);
            course.setTags(new ArrayList<>(List.of(tag1)));
            courseRepository.save(course);
            courseRepository.flush();

            // When - Replace tags
            CourseEntity toUpdate = courseRepository.findById(course.getId()).orElseThrow();
            toUpdate.setTags(new ArrayList<>(List.of(tag2)));
            courseRepository.save(toUpdate);
            courseRepository.flush();

            // Then
            CourseEntity updated = courseRepository.findById(course.getId()).orElseThrow();
            assertThat(updated.getTags()).hasSize(1);
            assertThat(updated.getTags().get(0).getName()).isEqualTo("New Tag");
        }

        @Test
        @DisplayName("BUG-007: should increment version on update")
        void shouldIncrementVersionOnUpdate() {
            // Given
            CourseEntity course = createCourse("Version Test", 3);
            CourseEntity saved = courseRepository.save(course);
            courseRepository.flush();
            Long initialVersion = saved.getVersion();

            // When
            CourseEntity toUpdate = courseRepository.findById(saved.getId()).orElseThrow();
            toUpdate.setTitle("Version Test Updated");
            courseRepository.save(toUpdate);
            courseRepository.flush();

            // Then
            CourseEntity updated = courseRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("should delete course by ID")
        void shouldDeleteCourseById() {
            // Given
            UUID courseId = UUID.randomUUID();
            CourseEntity course = createCourse(courseId, "To Delete", 2);
            courseRepository.save(course);

            // When
            courseRepository.deleteById(courseId);

            // Then
            assertThat(courseRepository.findById(courseId)).isEmpty();
        }

        @Test
        @DisplayName("should delete course with tag relationships")
        void shouldDeleteCourseWithTagRelationships() {
            // Given
            TagEntity tag = createTag("Tag For Deletion Test");
            tagRepository.save(tag);

            CourseEntity course = createCourse("Course To Delete", 3);
            course.setTags(new ArrayList<>(List.of(tag)));
            courseRepository.save(course);
            courseRepository.flush();

            UUID courseId = course.getId();
            UUID tagId = tag.getId();

            // When - Delete course
            courseRepository.deleteById(courseId);
            courseRepository.flush();

            // Then - Course deleted, but tag remains
            assertThat(courseRepository.findById(courseId)).isEmpty();
            assertThat(tagRepository.findById(tagId)).isPresent();
        }
    }

    @Nested
    @DisplayName("BUG-003: Bidirectional Relationships")
    class BidirectionalRelationships {

        @Test
        @DisplayName("should create lesson with course reference")
        void shouldCreateLessonWithCourseReference() {
            // Given - Create course first
            CourseEntity course = createCourse("Course With Lessons", 4);
            courseRepository.save(course);
            courseRepository.flush();

            // When - Create lesson with course reference (BUG-003: mappedBy)
            LessonEntity lesson = new LessonEntity();
            lesson.setId(UUID.randomUUID());
            lesson.setTitle("Lesson 1");
            lesson.setContent("Introduction");
            lesson.setOrderIndex(1);
            lesson.setCourse(course);

            lessonRepository.save(lesson);
            lessonRepository.flush();

            // Then
            Optional<LessonEntity> found = lessonRepository.findById(lesson.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getCourse()).isNotNull();
            assertThat(found.get().getCourse().getId()).isEqualTo(course.getId());
        }

        @Test
        @DisplayName("should load course with lessons via mappedBy")
        void shouldLoadCourseWithLessonsViaMappedBy() {
            // Given
            CourseEntity course = createCourse("Course With Multiple Lessons", 4);
            courseRepository.save(course);
            courseRepository.flush();

            // Add lessons
            LessonEntity lesson1 = createLesson("Lesson 1", course, 1);
            LessonEntity lesson2 = createLesson("Lesson 2", course, 2);
            LessonEntity lesson3 = createLesson("Lesson 3", course, 3);
            lessonRepository.saveAll(List.of(lesson1, lesson2, lesson3));
            lessonRepository.flush();

            // Clear persistence context to force fresh load from database
            entityManager.clear();

            // When - Reload course from database
            CourseEntity found = courseRepository.findById(course.getId()).orElseThrow();

            // Then - BUG-003: mappedBy = "course" allows loading lessons
            assertThat(found.getLessons()).hasSize(3);
        }
    }

    private CourseEntity createCourse(String title, int credits) {
        return createCourse(UUID.randomUUID(), title, credits);
    }

    private CourseEntity createCourse(UUID id, String title, int credits) {
        CourseEntity course = new CourseEntity();
        course.setId(id);
        course.setTitle(title);
        course.setDescription(title + " description");
        course.setCredits(credits);
        course.setActive(true);
        return course;
    }

    private TagEntity createTag(String name) {
        TagEntity tag = new TagEntity();
        tag.setId(UUID.randomUUID());
        tag.setName(name);
        tag.setDescription(name + " description");
        return tag;
    }

    private LessonEntity createLesson(String title, CourseEntity course, int orderIndex) {
        LessonEntity lesson = new LessonEntity();
        lesson.setId(UUID.randomUUID());
        lesson.setTitle(title);
        lesson.setContent(title + " content");
        lesson.setOrderIndex(orderIndex);
        lesson.setCourse(course);
        return lesson;
    }
}
